package me.fzzy.robofzzy4j

import javafx.util.Pair

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

    fun setValue(id: Long, newValue: Int): LeaderboardChange {
        return if (valueMap.containsKey(id)) {
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

    private fun newEntry(id: Long, newValue: Int): LeaderboardChange {

        // Start from the bottom of the leaderboard
        val rank = valueMap.size + 1
        valueMap[id] = Pair(rank, newValue)
        rankMap[rank] = id
        return setValue(id, newValue)
    }

    private fun moveUpInLeaderboard(id: Long, newValue: Int): LeaderboardChange {
        var rank = valueMap[id]!!.key

        val changes = LeaderboardChange()
        // If the new value is greater than the entry 1 rank above it, move it, repeat
        var compare = rankMap[rank - 1]
        while (rank != 1 && newValue > valueMap[compare]!!.value) {
            changes.moveDown(compare!!)
            changes.moveUp(id)

            valueMap[compare] = Pair(rank, valueMap[compare]!!.value)
            rankMap[rank] = compare
            rank--
            rankMap[rank] = id
            compare = rankMap[rank - 1]
        }
        valueMap[id] = Pair(rank, newValue)
        return changes
    }

    private fun moveDownInLeaderboard(id: Long, newValue: Int): LeaderboardChange {
        var rank = valueMap[id]!!.key

        val changes = LeaderboardChange()
        // If the new value is less than the entry 1 rank below it, move it, repeat
        var compare = rankMap[rank + 1]
        while (rank != valueMap.size && newValue < valueMap[compare]!!.value) {
            changes.moveUp(compare!!)
            changes.moveDown(id)

            valueMap[compare] = Pair(rank, valueMap[compare]!!.value)
            rankMap[rank] = compare
            rank++
            rankMap[rank] = id
            compare = rankMap[rank + 1]
        }
        valueMap[id] = Pair(rank, newValue)
        return changes
    }

    class LeaderboardChange {
        val positions = HashMap<Long, Int>()

        fun moveUp(id: Long) {
            val amt = positions.getOrDefault(id, 0)
            positions[id] = amt + 1
        }

        fun moveDown(id: Long) {
            val amt = positions.getOrDefault(id, 0)
            positions[id] = amt - 1
        }

        fun add(change: LeaderboardChange) {
            for ((id, c) in change.positions) {
                positions[id] = positions.getOrDefault(id, 0) + c
            }
        }
    }
}