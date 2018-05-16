package me.fzzy.eventvoter

class Task : Thread() {

    private val autoSave = 120
    private var autoSaveCount = 0

    override fun run() {
        while (true) {
            Thread.sleep(1000)
            if (++autoSaveCount >= autoSave) {
                autoSaveCount = 0
                for (leaderboard in guilds) {
                    leaderboard.saveLeaderboard()
                }
            }
        }
    }

}