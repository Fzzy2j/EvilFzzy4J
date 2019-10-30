package me.fzzy.evilfzzy4j.command.economy

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandCost
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.awt.Color
import java.util.*
import java.util.concurrent.TimeUnit

object LeaderboardCommand : Command("leaderboard") {

    override val cooldownMillis: Long = 10 * 1000
    override val votes: Boolean = false
    override val description = "shows the vote leaderboard"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = false
    override val price: Int = 1
    override val cost: CommandCost = CommandCost.COOLDOWN

    private const val title = "LEADERBOARD"

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {
        val guild = FzzyGuild.getGuild(event.guild.id)
        val sorted = guild.getSortedCurrency()

        val embedBuilder = EmbedBuilder()
        embedBuilder.setTitle(title)
        embedBuilder.setColor(Color(0, 200, 255))
        embedBuilder.setThumbnail("https://i.gyazo.com/5227ef31b9cdbc11d9f1e7313872f4af.gif")
        var i = 1
        for ((id, value) in sorted) {
            val member = event.guild.getMemberById(id)
            if (id == 0L || member == null || id == Bot.client.selfUser.idLong) continue
            val title = "#$i - ${member.effectiveName}"
            val description = "$value ${Bot.currencyEmoji.asMention}"
            embedBuilder.addField(title, description, false)
            i++
        }

        event.channel.sendMessage(embedBuilder.build()).queue { msg -> msg.delete().queueAfter(2, TimeUnit.MINUTES) }

        return CommandResult.success()
    }

}