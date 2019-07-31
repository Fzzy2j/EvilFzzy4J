package me.fzzy.evilfzzy4j.command.economy

import discord4j.core.`object`.util.Snowflake
import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandCost
import me.fzzy.evilfzzy4j.command.CommandResult
import reactor.core.publisher.Mono
import java.awt.Color
import java.util.*

object LeaderboardCommand : Command("leaderboard") {

    override val cooldownMillis: Long = 10 * 1000
    override val votes: Boolean = false
    override val description = "shows the vote leaderboard"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = false
    override val price: Int = 1
    override val cost: CommandCost = CommandCost.COOLDOWN

    private const val title = "LEADERBOARD"

    override fun runCommand(message: CachedMessage, args: List<String>): Mono<CommandResult> {
        val guild = FzzyGuild.getGuild(message.guild.id)
        val sorted = guild.getSortedCurrency()

        message.channel.createEmbed { spec ->
            spec.setTitle(title)
            spec.setColor(Color(0, 200, 255))
            spec.setThumbnail("https://i.gyazo.com/5227ef31b9cdbc11d9f1e7313872f4af.gif")
            var i = 1
            for ((id, value) in sorted) {

                val title = "#$i - ${guild.getDiscordGuild().getMemberById(Snowflake.of(id)).block()!!.displayName}"
                val description = "$value ${Bot.toUsable(Bot.currencyEmoji)}"
                spec.addField(title, description, false)
                i++
            }
        }.block()
        return Mono.just(CommandResult.success())
    }

}