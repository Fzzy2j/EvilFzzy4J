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
    override val usageText: String = "leaderboard"
    override val allowDM: Boolean = false
    override val cost: Int = 1

    private const val title = "LEADERBOARD - resets every monday"

    override fun runCommand(message: IMessage, args: List<String>): CommandResult {
        val builder = EmbedBuilder()
        val guild = Guild.getGuild(message.guild.longID)
        for (i in 1..25) {
            val id = guild.leaderboard.getAtRank(i)
            if (id != null) {
                val value = guild.leaderboard.getOrDefault(id, 0)

                val title = "#$i - ${Bot.client.getUserByID(id).getDisplayName(Bot.client.getGuildByID(message.guild.longID))} | ${User.getUser(id).getCooldownModifier(guild)}% CDR"
                val description = "$value points"
                builder.appendField(title, description, false)
            }
        }

        builder.withTitle(title)

        builder.withColor(0, 200, 255)
        builder.withThumbnail("https://i.gyazo.com/5227ef31b9cdbc11d9f1e7313872f4af.gif")

        var existingLeaderboard: IMessage? = null
        for (msg in message.channel.getMessageHistory(5)) {
            if (msg.embeds.isEmpty()) continue
            if (msg.author.longID != Bot.client.ourUser.longID) continue
            if (msg.embeds[0].title == title) {
                existingLeaderboard = msg
                break
            }
        }

        if (existingLeaderboard != null)
            RequestBuffer.request { existingLeaderboard.edit(builder.build()) }
        else
            RequestBuffer.request { MessageScheduler.sendTempEmbed(Bot.DEFAULT_TEMP_MESSAGE_DURATION, message.channel, builder.build()) }
        return CommandResult.success()
    }

}