package me.fzzy.robofzzy4j.commands.help

import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.MessageScheduler
import me.fzzy.robofzzy4j.util.CommandCost
import me.fzzy.robofzzy4j.util.CommandResult
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.io.File

object Eyetypes : Command {

    override val cooldownMillis: Long = 4 * 1000
    override val votes: Boolean = false
    override val description: String = "Shows all the eyes the bot can place on an image using the -eyes command"
    override val usageText: String = "eyetypes"
    override val allowDM: Boolean = true
    override val price: Int = 0
    override val cost: CommandCost = CommandCost.CURRENCY

    override fun runCommand(message: IMessage, args: List<String>): CommandResult {
        var all = "```"
        for (file in File("eyes").listFiles()) {
            all += "-eyes ${file.nameWithoutExtension.replace("_mirror", "")}\n"
        }
        all += "```"
        RequestBuffer.request {
            try {
                message.author.orCreatePMChannel.sendMessage(all)
            } catch (e: MissingPermissionsException) {
                MessageScheduler.sendTempMessage(Bot.data.DEFAULT_TEMP_MESSAGE_DURATION, message.channel, "${message.author.mention()} i dont have permission to tell you about what i can do :(")
            }
        }
        return CommandResult.success()
    }

}