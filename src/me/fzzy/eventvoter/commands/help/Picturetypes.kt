package me.fzzy.eventvoter.commands.help

import me.fzzy.eventvoter.Command
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.RequestBuffer
import java.io.File

class Picturetypes : Command {

    override val cooldownMillis: Long = 4 * 1000
    override val attemptDelete: Boolean = true
    override val description: String = "Shows all the picturess the bot can insert an image into using the -picture command"
    override val usageText: String = "-picturetypes"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        var all = "```"
        for (file in File("pictures").listFiles()) {
            all += "-picture ${file.nameWithoutExtension} [imageUrl]\n"
        }
        all += "```"
        RequestBuffer.request { event.message.author.orCreatePMChannel.sendMessage(all) }
    }

}