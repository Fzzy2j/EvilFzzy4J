package me.fzzy.robofzzy4j.commands.help

import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.CommandHandler
import me.fzzy.robofzzy4j.MessageScheduler
import me.fzzy.robofzzy4j.util.CommandResult
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer

object Help : Command {

    override val cooldownCategory = "help"
    override val cooldownMillis: Long = 4 * 1000
    override val votes: Boolean = false
    override val description = "The help command"
    override val usageText: String = "help"
    override val allowDM: Boolean = true
    override val cost: Int = 100

    override fun runCommand(message: IMessage, args: List<String>): CommandResult {
        var helpMsg = "```md\n"
        for ((_, command) in CommandHandler.getAllCommands()) {
            helpMsg += "# ${Bot.data.BOT_PREFIX}${command.usageText}\n${command.description} : ${command.cooldownMillis / 1000} second cooldown\n\n"
        }
        helpMsg += "```"
        RequestBuffer.request {
            try {
                message.author.orCreatePMChannel.sendMessage(helpMsg)
            } catch (e: MissingPermissionsException) {
                MessageScheduler.sendTempMessage(Bot.data.DEFAULT_TEMP_MESSAGE_DURATION, message.channel, "${message.author.mention()} i dont have permission to tell you about what i can do :(")
            }
        }

        return CommandResult.success()
    }

}