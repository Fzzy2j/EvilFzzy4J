package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.CommandResult
import me.fzzy.robofzzy4j.Guild
import me.fzzy.robofzzy4j.RoboFzzy
import me.fzzy.robofzzy4j.listeners.VoteListener
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionRemoveEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer

object Vote : Command {

    override val cooldownMillis: Long = 1000 * 60 * 5
    override val description: String = "Puts a message in the chat that allows users to vote on something"
    override val votes: Boolean = false
    override val usageText: String = "-vote [message]"
    override val allowDM: Boolean = false

    private const val BEGINNING = "```diff\n- Votes - "

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        if (args.isEmpty()) return CommandResult.fail("i should really put a message on that $usageText")
        updateOrSendAttendanceMessage(event.channel, args.joinToString(" "))
        return CommandResult.success()
    }

    fun getAttendanceMessage(channel: IChannel): IMessage? {
        for (msg in channel.getMessageHistory(20)) {
            if (msg.content.contains(BEGINNING) && msg.author.longID == RoboFzzy.cli.ourUser.longID) {
                return msg
            }
        }
        return null
    }

    fun extractMessage(msg: String): String {
        return msg.substring(BEGINNING.length, msg.indexOf("\n\nYeah\n"))
    }

    fun updateOrSendAttendanceMessage(channel: IChannel, message: String = "") {
        val msg = getAttendanceMessage(channel)
        var content = message
        if (msg != null) {
            content = extractMessage(msg.content)
        }
        var t = "$BEGINNING$content"
        t += "\n\nYeah\n"
        if (msg != null) {
            for (attend in VoteListener.getUpvoters(msg)) {
                t += "+ ${attend.name}\n"
            }
        }
        t += "\n\nNo\n"
        if (msg != null) {
            for (noshow in VoteListener.getDownvoters(msg)) {
                t += "- ${noshow.name}\n"
            }
        }
        t += "```"
        if (msg == null) {
            RequestBuffer.request { Guild.getGuild(channel.guild).allowVotes(channel.sendMessage(t)) }
        } else {
            RequestBuffer.request { msg.edit(t) }
        }
    }

    @EventSubscriber
    fun onReactionAdd(event: ReactionAddEvent) {
        if (event.message.author.longID == RoboFzzy.cli.ourUser.longID && !event.user.isBot) {
            if (getAttendanceMessage(event.channel) != null)
                updateOrSendAttendanceMessage(event.channel)
        }
    }

    @EventSubscriber
    fun onReactionRemove(event: ReactionRemoveEvent) {
        if (event.message.author.longID == RoboFzzy.cli.ourUser.longID && !event.user.isBot) {
            if (getAttendanceMessage(event.channel) != null)
                updateOrSendAttendanceMessage(event.channel)
        }
    }
}