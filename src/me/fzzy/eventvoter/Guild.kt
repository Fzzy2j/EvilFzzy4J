package me.fzzy.eventvoter

import java.io.File

class Guild constructor(var guildId: Long) {

    val longId: Long = this.guildId
    val leaderboard: Leaderboard = Leaderboard(guildId)

    var posts = 0
    var votes = 0

    private val file: File = File("$guildId.txt")

    fun getAverageVote(): Int {
        return (posts.toFloat() / votes.toFloat()).toInt()
    }

    fun save() {
        if (leaderboard.valueMap.size > 0) {
            var serial = ""

            for ((key, value) in leaderboard.valueMap) {
                serial += ";$key,${value.value},"
            }

            serial += "%$posts,$votes,"

            file.printWriter().use { out -> out.println(serial.substring(1)) }
        } else
            file.printWriter().use { out -> out.println() }
    }

    fun load() {
        leaderboard.clear()
        if (file.exists()) {
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
        }
    }


}