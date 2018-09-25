package me.fzzy.robofzzy4j

import java.io.File

class Guild constructor(private var guildId: Long) {

    val longId: Long = this.guildId
    val leaderboard: Leaderboard = Leaderboard(guildId)

    var posts = 0
    var votes = 0

    fun getAverageVote(): Int {
        return (posts.toFloat() / votes.toFloat()).toInt()
    }

    fun save() {
        if (leaderboard.valueMap.size > 0) {

            var i= 0
            for ((key, value) in leaderboard.valueMap) {
                guildNode.getNode("votes", i, "id").value = key
                guildNode.getNode("votes", i, "value").value = value.value
                i++
            }

            guildNode.getNode("totalVotes").value = votes
            guildNode.getNode("totalPosts").value = posts

            guildManager.save(guildNode)
        }
    }

    fun load() {
        leaderboard.clear()
        for  (node in guildNode.getNode("votes").childrenList) {
            leaderboard.setValue(node.getNode("id").long, node.getNode("value").int)
        }
        /*if (file.exists()) {
            val serial = file.readText()
            if (serial.split("%")[0].split(";")[0].length > 2) {
                for (score in serial.split("%")[0].split(";")) {
                    val id = score.split(",")[0].toLong()
                    val value = score.split(",")[1].toInt()
                    leaderboard.setValue(id, value)
                }
                posts = serial.split("%")[1].split(",")[0].toInt()
                votes = serial.split("%")[1].split(",")[1].toInt()
            }
        }*/
    }


}