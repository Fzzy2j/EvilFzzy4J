package me.fzzy.robofzzy4j

import javafx.util.Pair
import java.util.*

class Leaderboard {

    //valueMap[id]=Pair(Rank, Score)
    var valueMap: HashMap<Long, Pair<Int, Int>> = HashMap()
        private set
    //rankMap[Rank]=id
    private var rankMap: HashMap<Int, Long> = HashMap()

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

    fun setValue(id: Long, newValue: Int): List<Long> {
        if (valueMap.containsKey(id)) {
            val prevValue = valueMap[id]!!.value

            // Determines if they need to move up or down in the leaderboard
            if (newValue < prevValue) {
                moveDownInLeaderboard(id, newValue)
            } else {
                return moveUpInLeaderboard(id, newValue)
            }
        } else {
            newEntry(id, newValue)
        }
        return arrayListOf()
    }

    private fun newEntry(id: Long, newValue: Int) {

        // Start from the bottom of the leaderboard
        val rank = valueMap.size + 1
        valueMap[id] = Pair(rank, newValue)
        rankMap[rank] = id
        setValue(id, newValue)
    }

    private fun moveUpInLeaderboard(id: Long, newValue: Int): List<Long> {
        var rank = valueMap[id]!!.key

        val passed = arrayListOf<Long>()
        // If the new value is greater than the entry 1 rank above it, move it, repeat
        var compare = rankMap[rank - 1]
        while (rank != 1 && newValue > valueMap[compare]!!.value) {
            passed.add(compare!!)

            valueMap[compare] = Pair(rank, valueMap[compare]!!.value)
            rankMap[rank] = compare
            rank--
            rankMap[rank] = id
            compare = rankMap[rank - 1]
        }
        valueMap[id] = Pair(rank, newValue)
        return passed
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
}