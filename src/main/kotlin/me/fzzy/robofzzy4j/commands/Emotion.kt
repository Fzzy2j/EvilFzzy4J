package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import org.json.JSONObject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.net.URL

object Emotion : Command {

    override val cooldownCategory = "image"
    override val cooldownMillis: Long = 60 * 1000 * 3
    override val votes: Boolean = false
    override val description = "Displays the emotion of faces in an image"
    override val usageText: String = "-emotion [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)

        val url: URL? = ImageFuncs.getFirstImage(history) ?: return CommandResult.fail("i couldnt find an image in the last 10 messages")
        val faces = ImageFuncs.getFacialInfo("emotion", false, false, url.toString())
                ?: return CommandResult.fail("the face api isnt working, i dont know why")

        if (faces.length() == 0)
            return CommandResult.fail("the api couldnt find any faces in that picture")

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