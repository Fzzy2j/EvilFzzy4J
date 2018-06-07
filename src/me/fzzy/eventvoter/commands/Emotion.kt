package me.fzzy.eventvoter.commands

import me.fzzy.eventvoter.*
import me.fzzy.eventvoter.thread.ImageProcessTask
import org.json.JSONArray
import org.json.JSONObject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.net.URL

class Emotion : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = true
    override val description = "Displays the emotion of faces in an image"
    override val usageText: String = "-emotion [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)
        val url: URL? = ImageFuncs.getFirstImage(history)
        if (url == null) {
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Couldn't find an image in the last 10 messages sent in this channel!") }
        } else {
            imageProcessQueue.addToQueue(ProcessImageEmotion({ ImageFuncs.getFacialInfo("emotion", false, false, url.toString()) }, event.channel))
        }
    }
}

private class ProcessImageEmotion(override val code: () -> Any?, private var channel: IChannel) : ImageProcessTask {

    var processingMessage: IMessage? = null

    override fun queueUpdated(position: Int) {
        val msg = if (position == 0) "processing..." else "position in queue: $position"
        RequestBuffer.request {
            if (processingMessage == null)
                processingMessage = Funcs.sendMessage(channel, msg)
            else
                processingMessage?.edit(msg)
        }
    }

    override fun finished(obj: Any?) {
        if (obj == null) {
            processingMessage?.delete()
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, channel, "No faces detected in image.") }
        } else {
            if (obj is JSONArray) {
                if (obj.length() > 0) {
                    var finalOutput = ""
                    for (face in obj) {
                        if (face is JSONObject) {
                            var output = "```"
                            val map = face.getJSONObject("faceAttributes").getJSONObject("emotion").toMap()
                            for ((v1, v2) in map) {
                                output += "$v1: $v2\n"
                            }
                            output += "```"
                            finalOutput += output
                        }
                    }
                    processingMessage?.delete()
                    RequestBuffer.request { messageScheduler.sendTempMessage(60 * 1000, channel, finalOutput) }
                } else {
                    processingMessage?.delete()
                    RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, channel, "No faces detected in image.") }
                }
            }
        }
    }
}