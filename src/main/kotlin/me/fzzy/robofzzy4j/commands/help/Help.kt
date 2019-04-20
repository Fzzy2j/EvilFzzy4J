package me.fzzy.robofzzy4j.commands.help

import me.fzzy.robofzzy4j.*
import me.fzzy.robofzzy4j.util.Zalgo
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
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

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        var helpMsg = "```md\n"
        for ((_, command) in CommandHandler.getAllCommands()) {
            helpMsg += "# ${Bot.BOT_PREFIX}${command.usageText}\n${command.description} : ${command.cooldownMillis / 1000} second cooldown\n\n"
        }
        helpMsg += "```"
        RequestBuffer.request {
            try {
                event.author.orCreatePMChannel.sendMessage(helpMsg)
            } catch (e: MissingPermissionsException) {
                MessageScheduler.sendTempMessage(Bot.DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "${event.author.mention()} i dont have permission to tell you about what i can do :(")
            }
        }

        return CommandResult.success()
    }

}