package me.fzzy.eventvoter.commands

import me.fzzy.eventvoter.*
import org.json.JSONObject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.net.URL

class Emotion : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = false
    override val description = "Displays the emotion of faces in an image"
    override val usageText: String = "-emotion [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)
        val url: URL? = getFirstImage(history)
        if (url == null) {
            RequestBuffer.request { sendMessage(event.channel, "Couldn't find an image in the last 10 messages sent in this channel!") }
        } else {
            ProcessImageEmotion(url, event).start()
        }
    }
}

private class ProcessImageEmotion constructor(private var url: URL, private var event: MessageReceivedEvent) : Thread() {

    override fun run() {
        var processingMessage: IMessage? = null
        RequestBuffer.request { processingMessage = sendMessage(event.channel, "processing...") }

        val faces = getFacialInfo("emotion", false, false, url.toString())

        if (faces != null) {
            var finalOutput = ""
            if (faces.length() > 0) {
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
            } else {
                RequestBuffer.request {
                    val msg = sendMessage(event.channel, "No faces detected in image.")
                    if (msg != null)
                        TempMessage(7 * 1000, msg).start()
                }
            }
            RequestBuffer.request { sendMessage(event.channel, finalOutput) }
            processingMessage?.delete()
        }
    }
}