package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.Funcs.Companion.sendEmbed
import me.fzzy.robofzzy4j.cli
import me.fzzy.robofzzy4j.getGuild
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.RequestBuffer

class LeaderboardCommand : Command {

    override val cooldownMillis: Long = 4 * 1000
    override val attemptDelete: Boolean = true
    override val description = "shows the vote leaderboard"
    override val usageText: String = "-leaderboard"
    override val allowDM: Boolean = false

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        val builder = EmbedBuilder()
        for (i in 1..25) {
            val id = getGuild(event.guild.longID)?.leaderboard?.getAtRank(i)
            val value = getGuild(event.guild.longID)?.leaderboard?.getOrDefault(id!!, 0)

            val title = "#$i - ${cli.getUserByID(id!!).getDisplayName(cli.getGuildByID(event.guild.longID))}"
            val description = "$value points"
            builder.appendField(title, description, false)
        }

        builder.withAuthorName("LEADERBOARD")
        builder.withAuthorIcon("http://i.imgur.com/dYhgv64.jpg")

        builder.withColor(0, 200, 255)
        builder.withThumbnail("https://i.gyazo.com/5227ef31b9cdbc11d9f1e7313872f4af.gif")

        RequestBuffer.request { sendEmbed(event.channel, builder.build()) }
    }

}