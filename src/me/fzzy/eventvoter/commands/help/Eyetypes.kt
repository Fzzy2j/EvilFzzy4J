package me.fzzy.eventvoter.commands.help

import me.fzzy.eventvoter.Command
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.RequestBuffer
import java.io.File

class Eyetypes : Command {

    override val cooldownMillis: Long = 4 * 1000
    override val attemptDelete: Boolean = true
    override val description: String = "Shows all the eyes the bot can place on an image using the -eyes command"
    override val usageText: String = "-eyetypes"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        var all = "```"
        for (file in File("eyes").listFiles()) {
            all += "-eyes ${file.nameWithoutExtension} [imageUrl]\n"
        }
        all += "```"
        RequestBuffer.request { event.message.author.orCreatePMChannel.sendMessage(all) }
    }

}