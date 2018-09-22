package me.fzzy.robofzzy4j.listeners

import me.fzzy.robofzzy4j.*
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType

class StateListener {

    @EventSubscriber
    fun onReady(event: ReadyEvent) {
        for (guild in cli.guilds) {
            val leaderboard = Guild(guild.longID)
            guilds.add(leaderboard)
            leaderboard.load()
        }
        changeStatus(StatusType.DND, ActivityType.PLAYING, "with fire ${BOT_PREFIX}help")
    }

}