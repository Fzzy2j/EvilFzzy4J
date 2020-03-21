package me.fzzy.evilfzzy4j.command

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color
import java.util.*
import java.util.concurrent.TimeUnit

object Leaderboard : Command("leaderboard") {

    override val cooldownMillis: Long = 10 * 1000
    override val description = "shows the vote leaderboard"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = false

    private const val title = "LEADERBOARD"

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {
        val guild = FzzyGuild.getGuild(event.guild.id)
        val sorted = guild.getSortedScores()

        val embedBuilder = EmbedBuilder()
        embedBuilder.setTitle(title)
        embedBuilder.setColor(Color(0, 200, 255))
        embedBuilder.setThumbnail("https://i.gyazo.com/fb2f077da35e0d39f9937689b01291c3.gif")
        var i = 1
        for ((id, value) in sorted) {
            val member = event.guild.getMemberById(id)
            if (id == 0L || member == null || id == Bot.client.selfUser.idLong) continue
            val title = "#$i - ${member.effectiveName}"
            val description = "$value points"
            embedBuilder.addField(title, description, false)
            i++
        }

        event.channel.sendMessage(embedBuilder.build()).queue { msg -> msg.delete().queueAfter(2, TimeUnit.MINUTES) }

        return CommandResult.success()
    }

}