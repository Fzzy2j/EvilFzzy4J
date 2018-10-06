package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.RequestBuffer
import java.io.File

class GetMeme : Command {

    override val cooldownMillis: Long = 60 * 1000
    override val votes: Boolean = false
    override val description = "Shows you upvoted posts from the server"
    override val usageText: String = "-getmeme"
    override val allowDM: Boolean = false

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {

        val files = File("memes", event.guild.longID.toString()).listFiles()
        if (files == null || files.isEmpty())
            return CommandResult.fail("there havent been any worthy memes in this server, sorry")
        val file = files[random.nextInt(files.size)]

        RequestBuffer.request {
            Funcs.sendFile(event.channel, file, false)
            file.delete()
        }
        return CommandResult.success()
    }
}