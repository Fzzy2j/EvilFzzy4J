package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
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

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)

        val url: URL? = ImageFuncs.getFirstImage(history) ?: return CommandResult.fail("Couldn't find image.")
        val faces = ImageFuncs.getFacialInfo("emotion", false, false, url.toString())
                ?: return CommandResult.fail("Couldn't contact API")

        if (faces.length() == 0)
            return CommandResult.fail("No faces detected in image.")

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
        RequestBuffer.request { Funcs.sendMessage(event.channel, finalOutput) }
        return CommandResult.success()
    }
}