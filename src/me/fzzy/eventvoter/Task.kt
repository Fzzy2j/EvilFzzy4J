package me.fzzy.eventvoter

import java.util.*

class Task : Thread() {

    override fun run() {
        while (running) {
            Thread.sleep(60 * 1000)

            println("auto-save for ${guilds.size} guilds")
            for (leaderboard in guilds) {
                if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                    if (leaderboard.weekWinner == null)
                        leaderboard.weekWinner = leaderboard.getCurrentWinner()
                    if (leaderboard.weekWinner != null) {
                        if (System.currentTimeMillis() - leaderboard.weekWinner!!.timestamp > 1000 * 60 * 60 * 24 * 3) {
                            leaderboard.weekWinner = leaderboard.getCurrentWinner()
                        }
                    }
                }

                leaderboard.saveLeaderboard()
                leaderboard.updateLeaderboard()
            }
        }
    }

}