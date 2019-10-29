package me.fzzy.evilfzzy4j.command.help

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandCost
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object Invite : Command("invite") {

    override val cooldownMillis: Long = 4 * 1000
    override val votes: Boolean = false
    override val description = "Gives you the invite link for the bot to add it to servers"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = true
    override val price: Int = 0
    override val cost: CommandCost = CommandCost.CURRENCY

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        event.author.openPrivateChannel().queue { private ->
            run {
                private.sendMessage(Bot.client.getInviteUrl(
                        Permission.MESSAGE_MANAGE,
                        Permission.MESSAGE_READ,
                        Permission.MESSAGE_WRITE,
                        Permission.MESSAGE_ATTACH_FILES,
                        Permission.MESSAGE_ADD_REACTION,
                        Permission.MESSAGE_EXT_EMOJI)).queue()
            }
        }
        return CommandResult.success()
    }

}