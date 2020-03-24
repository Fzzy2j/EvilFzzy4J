package me.fzzy.evilfzzy4j.command.help

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object Invite : Command("invite") {

    override val cooldownMillis: Long = 4 * 1000
    override val description = "Gives you the invite link for the bot to add it to servers"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {
        event.author.openPrivateChannel().queue { private ->
            run {
                private.sendMessage(Bot.client.getInviteUrl(Permission.ADMINISTRATOR)).queue()
            }
        }
        return CommandResult.success()
    }

}