package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.RequestBuffer

object LeaderboardCommand : Command {

    override val cooldownCategory = "leaderboard"
    override val cooldownMillis: Long = 10 * 1000
    override val votes: Boolean = false
    override val description = "shows the vote leaderboard"
    override val usageText: String = "-leaderboard"
    override val allowDM: Boolean = false

    private const val title = "LEADERBOARD - resets every monday"

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        val builder = EmbedBuilder()
        val guild = Guild.getGuild(event.guild.longID)
        for (i in 1..25) {
            val id = guild.leaderboard.getAtRank(i)
            if (id != null) {
                val value = guild.leaderboard.getOrDefault(id, 0)

                val title = "#$i - ${RoboFzzy.cli.getUserByID(id).getDisplayName(RoboFzzy.cli.getGuildByID(event.guild.longID))} | ${User.getUser(id).getCooldownModifier(guild)}% CDR"
                val description = "$value points"
                builder.appendField(title, description, false)
            }
        }

        builder.withTitle(title)

        builder.withColor(0, 200, 255)
        builder.withThumbnail("https://i.gyazo.com/5227ef31b9cdbc11d9f1e7313872f4af.gif")

        var existingLeaderboard: IMessage? = null
        for (msg in event.channel.getMessageHistory(5)) {
            if (msg.embeds.isEmpty()) continue
            if (msg.author.longID != RoboFzzy.cli.ourUser.longID) continue
            if (msg.embeds[0].title == title) {
                existingLeaderboard = msg
                break
            }
        }

        if (existingLeaderboard != null)
            RequestBuffer.request { existingLeaderboard.edit(builder.build()) }
        else
            RequestBuffer.request { Funcs.sendEmbed(event.channel, builder.build()) }
        return CommandResult.success()
    }

}