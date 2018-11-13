package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.RequestBuffer

object LeaderboardCommand : Command {

    override val cooldownMillis: Long = 10 * 1000
    override val votes: Boolean = false
    override val description = "shows the vote leaderboard"
    override val usageText: String = "-leaderboard"
    override val allowDM: Boolean = false

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

        builder.withAuthorName("LEADERBOARD - resets every monday")
        builder.withAuthorIcon(RoboFzzy.cli.ourUser.avatarURL)

        builder.withColor(0, 200, 255)
        builder.withThumbnail("https://i.gyazo.com/5227ef31b9cdbc11d9f1e7313872f4af.gif")

        RequestBuffer.request { Funcs.sendEmbed(event.channel, builder.build()) }
        return CommandResult.success()
    }

}