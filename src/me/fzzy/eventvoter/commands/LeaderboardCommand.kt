package me.fzzy.eventvoter.commands

import me.fzzy.eventvoter.Command
import me.fzzy.eventvoter.getLeaderboard
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

class LeaderboardCommand : Command {

    override val cooldownMillis: Long = 4 * 1000
    override val attemptDelete: Boolean = true
    override val description = "shows the vote leaderboard"
    override val usageText: String = "-leaderboard"
    override val allowDM: Boolean = false

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        getLeaderboard(event.guild.longID)?.sendLeaderboard(event.channel)
    }

}