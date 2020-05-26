package me.fzzy.evilfzzy4j.command.voice

import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import me.fzzy.evilfzzy4j.voice.FzzyPlayer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.ArrayList


object Stop : Command("stop") {

    override val cooldownMillis: Long = 10 * 1000
    override val description = "stop audio"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = false

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {
        FzzyPlayer.getPlayer(event.guild).player.stopTrack()
        return CommandResult.success()
    }

}