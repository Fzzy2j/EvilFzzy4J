package me.fzzy.robofzzy4j.listeners

import me.fzzy.robofzzy4j.*
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.RequestBuffer

object StateListener {

    @EventSubscriber
    fun onReady(event: ReadyEvent) {
        RequestBuffer.request { RoboFzzy.cli.changePresence(StatusType.ONLINE, ActivityType.LISTENING, "the rain ${RoboFzzy.BOT_PREFIX}help") }
    }

}