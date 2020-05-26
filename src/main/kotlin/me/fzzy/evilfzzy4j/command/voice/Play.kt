package me.fzzy.evilfzzy4j.command.voice

import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import me.fzzy.evilfzzy4j.voice.FzzyPlayer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.ArrayList

object Play : Command("play") {

    override val cooldownMillis: Long = 10 * 1000
    override val description = "play audio"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = false

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {
        val guild = event.guild
        val voiceState = guild.getMember(event.author)?.voiceState?: return CommandResult.fail("caching is disabled, not my fault")
        val channel = voiceState.channel?: return CommandResult.fail("you must be in a voice channel to use this command")

        val player = FzzyPlayer.getPlayer(guild)
        player.play(channel, args[0])
        return CommandResult.success()
    }
}