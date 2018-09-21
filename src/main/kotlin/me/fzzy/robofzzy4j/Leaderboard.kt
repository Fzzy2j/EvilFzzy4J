package me.fzzy.robofzzy4j

import javafx.util.Pair
import java.util.*

class Leaderboard constructor(private var guildId: Long) {

    //valueMap[id]=Pair(Rank, Score)
    var valueMap: HashMap<Long, Pair<Int, Int>> = HashMap()
        private set
    //rankMap[Rank]=id
    private var rankMap: HashMap<Int, Long> = HashMap()

    val leaderboardGuildId: Long get() = this.guildId

    fun clear() {
        rankMap.clear()
        valueMap.clear()
    }

    fun getOrDefault(id: Long, def: Int): Int {
        return if (valueMap.containsKey(id)) valueMap[id]!!.value else def
    }

    fun getAtRank(rank: Int): Long? {
        return rankMap[rank]
    }

    fun getRank(id: Long): Int? {
        return valueMap[id]?.key
    }

    fun setValue(id: Long, newValue: Int) {
        if (valueMap.containsKey(id)) {
            val prevValue = valueMap[id]!!.value

            // Determines if they need to move up or down in the leaderboard
            if (newValue < prevValue) {
                moveDownInLeaderboard(id, newValue)
            } else {
                moveUpInLeaderboard(id, newValue)
            }
        } else {
            newEntry(id, newValue)
        }
    }

    private fun newEntry(id: Long, newValue: Int) {

        // Start from the bottom of the leaderboard
        val rank = valueMap.size + 1
        valueMap[id] = Pair(rank, newValue)
        rankMap[rank] = id
        setValue(id, newValue)
    }

    private fun moveUpInLeaderboard(id: Long, newValue: Int) {
        var rank = valueMap[id]!!.key

        // If the new value is greater than the entry 1 rank above it, move it, repeat
        var compare = rankMap[rank - 1]
        while (rank != 1 && newValue > valueMap[compare]!!.value) {
            valueMap[compare!!] = Pair(rank, valueMap[compare]!!.value)
            rankMap[rank] = compare
            rank--
            rankMap[rank] = id
            compare = rankMap[rank - 1]
        }
        valueMap[id] = Pair(rank, newValue)
    }

    private fun moveDownInLeaderboard(id: Long, newValue: Int) {
        var rank = valueMap[id]!!.key

        // If the new value is less than the entry 1 rank below it, move it, repeat
        var compare = rankMap[rank + 1]
        while (rank != valueMap.size && newValue < valueMap[compare]!!.value) {
            valueMap[compare!!] = Pair(rank, valueMap[compare]!!.value)
            rankMap[rank] = compare
            rank++
            rankMap[rank] = id
            compare = rankMap[rank + 1]
        }
        valueMap[id] = Pair(rank, newValue)
    }

    /*private var scores: HashMap<Long, Int>

    init {
        scores = hashMapOf()
    }

    val leaderboardGuildId: Long get() = this.guildId
    private val file: File = File("$guildId.txt")

    fun addToScore(id: Long, amt: Int) {
        scores[id] = scores.getOrDefault(id, 0) + amt
    }

    fun clearLeaderboard() {
        scores = hashMapOf()
    }

    fun saveLeaderboard() {
        if (scores.size > 0) {
            var serial = ""

            for ((key, value) in scores) {
                serial += ";$key,$value,"
            }

            file.printWriter().use { out -> out.println(serial.substring(1)) }
        } else
            file.printWriter().use { out -> out.println() }
    }

    fun loadLeaderboard() {
        scores = hashMapOf()
        if (file.exists()) {
            val serial = file.readText()
            if (serial.split("%")[0].split(";")[0].length > 2) {
                for (score in serial.split("%")[0].split(";")) {
                    val id = score.split(",")[0].toLong()
                    val value = score.split(",")[1].toInt()
                    scores[id] = value
                }
            }
        }
    }

    fun sendLeaderboard(channel: IChannel) {
        val builder = EmbedBuilder()

        var count = 0
        for ((key, value) in getSortedLeaderboard()) {
            if (++count > 25)
                break
            val title = "#$count - ${cli.getUserByID(key).getDisplayName(cli.getGuildByID(guildId))}"
            val description = "$value points"
            builder.appendField(title, description, false)
        }

        builder.withAuthorName("LEADERBOARD")
        builder.withAuthorIcon("http://i.imgur.com/dYhgv64.jpg")

        builder.withColor(0, 200, 255)
        builder.withThumbnail("https://i.gyazo.com/5227ef31b9cdbc11d9f1e7313872f4af.gif")

        RequestBuffer.request { messageScheduler.sendTempEmbed(10000, channel, builder.build()) }
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
    }*/
}