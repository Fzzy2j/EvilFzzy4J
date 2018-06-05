package me.fzzy.eventvoter

import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.util.*

private var presenceStatusType: StatusType? = null
private var presenceActivityType: ActivityType? = null
private var presenceText: String? = null

fun changeStatus(statusType: StatusType, activityType: ActivityType, text: String) {
    presenceStatusType = statusType
    presenceActivityType = activityType
    presenceText = text
    RequestBuffer.request { cli.changePresence(presenceStatusType, presenceActivityType, presenceText) }
}

class Task : Thread() {

    override fun run() {
        while (running) {
            Thread.sleep(60 * 1000)

            if (presenceActivityType != null && presenceStatusType != null && presenceText != null)
                RequestBuffer.request { cli.changePresence(presenceStatusType, presenceActivityType, presenceText) }

            println("auto-save for ${guilds.size} guilds")
            for (leaderboard in guilds) {
                if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                    if (leaderboard.weekWinner == null)
                        leaderboard.weekWinner = leaderboard.getCurrentWinner()
                    if (leaderboard.weekWinner != null) {
                        if (System.currentTimeMillis() - leaderboard.weekWinner!!.timestamp > 1000 * 60 * 60 * 25 * 1) {
                            leaderboard.weekWinner = leaderboard.getCurrentWinner()
                            leaderboard.clearLeaderboard()
                        }
                    }
                }

                leaderboard.saveLeaderboard()
                try {
                    cli.ourUser.getVoiceStateForGuild(cli.getGuildByID(leaderboard.leaderboardGuildId)).channel?.leave()
                    leaderboard.updateLeaderboard()
                } catch (e: MissingPermissionsException) {
                    println("Could not update leaderboard for guild")
                    e.printStackTrace()
                } catch (e: DiscordException) {
                    println("Could not update leaderboard for guild")
                    e.printStackTrace()
                }
            }
        }
    }

}