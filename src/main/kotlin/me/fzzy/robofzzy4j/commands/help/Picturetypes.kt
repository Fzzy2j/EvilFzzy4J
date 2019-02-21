package me.fzzy.robofzzy4j.commands.help

import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.CommandResult
import me.fzzy.robofzzy4j.MessageScheduler
import me.fzzy.robofzzy4j.RoboFzzy
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.io.File

object Picturetypes : Command {

    override val cooldownCategory = "help"
    override val cooldownMillis: Long = 4 * 1000
    override val votes: Boolean = false
    override val description: String = "Shows all the picturess the bot can insert an image into using the -picture command"
    override val usageText: String = "-picturetypes"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        var all = "```"
        for (file in File("pictures").listFiles()) {
            all += "-picture ${file.nameWithoutExtension} [imageUrl]\n"
        }
        all += "```"
        RequestBuffer.request {
            try {
                event.message.author.orCreatePMChannel.sendMessage(all)
            } catch (e: MissingPermissionsException) {
                MessageScheduler.sendTempMessage(RoboFzzy.DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "${event.author.mention()} i dont have permission to tell you about what i can do :(")
            }
        }
        return CommandResult.success()
    }

}