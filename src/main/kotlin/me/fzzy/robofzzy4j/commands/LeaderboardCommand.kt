package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.Guild
import me.fzzy.robofzzy4j.util.CommandCost
import me.fzzy.robofzzy4j.util.CommandResult
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.RequestBuffer
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

    override fun runCommand(message: IMessage, args: List<String>): CommandResult {
        val builder = EmbedBuilder()
        val guild = Guild.getGuild(message.guild.longID)
        val sorted = guild.currency.toSortedMap(compareBy { -guild.currency[it]!! })
        var i = 1
        for ((id, value) in sorted) {
            val title = "#$i - ${Bot.client.getUserByID(id).getDisplayName(Bot.client.getGuildByID(message.guild.longID))}"
            val description = "$value ${Bot.CURRENCY_EMOJI}"
            builder.appendField(title, description, false)
            i++
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
            RequestBuffer.request { Bot.sendEmbed(message.channel, builder.build()) }
        return CommandResult.success()
    }

    fun sortHashMapByValues(passedMap: java.util.HashMap<Long, Int>): LinkedHashMap<Long, Int> {
        val mapKeys = ArrayList(passedMap.keys)
        val mapValues = ArrayList(passedMap.values)
        mapValues.sort()
        mapKeys.sort()

        val sortedMap = LinkedHashMap<Long, Int>()

        val valueIt = mapValues.iterator()
        while (valueIt.hasNext()) {
            val `val` = valueIt.next()
            val keyIt = mapKeys.iterator()

            while (keyIt.hasNext()) {
                val key = keyIt.next()
                val comp1 = passedMap[key]

                if (comp1 == `val`) {
                    keyIt.remove()
                    sortedMap[key] = `val`
                    break
                }
            }
        }
        return sortedMap
    }

}