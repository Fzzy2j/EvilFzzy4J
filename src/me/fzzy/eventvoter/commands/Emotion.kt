package me.fzzy.eventvoter.commands

import me.fzzy.eventvoter.*
import me.fzzy.eventvoter.thread.ImageProcessTask
import org.json.JSONObject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
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
            imageProcessQueue.addToQueue(object: ImageProcessTask{

                var processingMessage: IMessage? = null
                override fun run(): Any? {
                    val faces = ImageFuncs.getFacialInfo("emotion", false, false, url.toString())
                    if (faces == null) {
                        processingMessage?.delete()
                        RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Could not contact API.") }
                    } else {
                        if (faces.length() > 0) {
                            var finalOutput = ""
                            for (face in faces) {
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
                            RequestBuffer.request { messageScheduler.sendTempMessage(60 * 1000, event.channel, finalOutput) }
                        } else {
                            processingMessage?.delete()
                            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "No faces detected in image.") }
                        }
                    }
                    return faces
                }

                override fun queueUpdated(position: Int) {
                    val msg = if (position == 0) "processing..." else "position in queue: $position"
                    RequestBuffer.request {
                        if (processingMessage == null)
                            processingMessage = Funcs.sendMessage(event.channel, msg)
                        else
                            processingMessage?.edit(msg)
                    }
                }
            })
        }
    }
}