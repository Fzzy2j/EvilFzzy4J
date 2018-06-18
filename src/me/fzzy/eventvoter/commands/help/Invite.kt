package me.fzzy.eventvoter.commands.help

import me.fzzy.eventvoter.Command
import me.fzzy.eventvoter.cli
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.RequestBuffer

class Invite : Command {

    override val cooldownMillis: Long = 4 * 1000
    override val attemptDelete: Boolean = true
    override val description = "Gives you the invite link for the bot to add it to servers"
    override val usageText: String = "-invite"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        RequestBuffer.request { event.author.orCreatePMChannel.sendMessage("https://discordapp.com/oauth2/authorize?client_id=${cli.ourUser.longID}&scope=bot&permissions=306240") }
    }

}