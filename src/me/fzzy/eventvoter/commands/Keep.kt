package me.fzzy.eventvoter.commands

import me.fzzy.eventvoter.Command
import me.fzzy.eventvoter.cli
import me.fzzy.eventvoter.messageScheduler
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

class Keep : Command {

    override val cooldownMillis: Long = 4 * 1000
    override val attemptDelete: Boolean = true
    override val description = "finds the last temporary image sent and makes it permanent"
    override val usageText: String = "-keep"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        for (message in event.channel.getMessageHistory(10)) {
            if (message.attachments.size > 0) {
                if (message.author.longID == cli.ourUser.longID) {
                    messageScheduler.clearTempMessage(message)
                    break
                }
            }
        }
    }

}