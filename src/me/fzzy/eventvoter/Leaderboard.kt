package me.fzzy.eventvoter

import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.io.File
import java.util.ArrayList
import java.util.LinkedHashMap

class Leaderboard constructor(private var guildId: Long) {

    var scores: HashMap<Long, Int>

    init {
        scores = hashMapOf()
    }

    fun getGuildId(): Long {
        return guildId
    }

    fun addToScore(id: Long, amt: Int) {
        scores[id] = scores.getOrDefault(id, 0) + amt
    }

    fun saveLeaderboard() {
        if (scores.size > 0) {
            var serial = ""
            for ((key, value) in scores) {
                serial += ";$key,$value,"
            }
            File("$guildId.txt").printWriter().use { out -> out.println(serial.substring(1)) }
        }
    }

    fun loadLeaderboard() {
        scores = hashMapOf()
        if (File("$guildId.txt").exists()) {
            val serial = File("$guildId.txt").readText()
            for (score in serial.split(";")) {
                val id = score.split(",")[0].toLong()
                val value = score.split(",")[1].toInt()
                scores[id] = value
            }
        }
    }

    fun updateLeaderboard() {
        val channel: MutableList<IChannel> = cli.getGuildByID(guildId).getChannelsByName("leaderboard")
        if (channel.size > 0) {
            val builder = EmbedBuilder()

            var count = 0
            for ((key, value) in getSortedLeaderboard()) {
                if (++count > 25)
                    break
                builder.appendField(cli.getUserByID(key).getDisplayName(cli.getGuildByID(guildId)), "$value", false)
            }

            builder.withAuthorName("LEADERBOARD")
            builder.withAuthorIcon("http://i.imgur.com/dYhgv64.jpg")

            builder.withColor(0, 200, 255)
            builder.withThumbnail("https://i.gyazo.com/5227ef31b9cdbc11d9f1e7313872f4af.gif")

            if (channel[0].getMessageHistory(1).size > 0 && channel[0].getMessageHistory(1)[0].author.longID == cli.ourUser.longID)
                RequestBuffer.request { channel[0].getMessageHistory(1)[0].edit(builder.build()) }
            else
                RequestBuffer.request {
                    try {
                        channel[0].sendMessage(builder.build())
                    } catch (e: MissingPermissionsException) {
                        // No permission to send message
                    }
                }
        }
    }

    fun getSortedLeaderboard(): LinkedHashMap<Long, Int> {
        val mapKeys = ArrayList(scores.keys)
        val mapValues = ArrayList(scores.values)
        mapValues.sortDescending()
        mapKeys.sort()

        val sortedMap = LinkedHashMap<Long, Int>()

        val valueIt = mapValues.iterator()
        while (valueIt.hasNext()) {
            val value = valueIt.next()
            val keyIt = mapKeys.iterator()

            while (keyIt.hasNext()) {
                val key = keyIt.next()
                val comp1 = scores[key]

                if (comp1 == value) {
                    keyIt.remove()
                    sortedMap[key] = value
                    break
                }
            }
        }
        return sortedMap
    }
}