package me.fzzy.evilfzzy4j.command.voice

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.net.URL

object Play : Command("play") {

    override val cooldownMillis: Long = 1000 * 60 * 10
    override val description = "Plays audio in the voice channel"
    override val args: ArrayList<String> = arrayListOf("url")
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {

        val state = event.member!!.voiceState?: return CommandResult.fail("i cant get your voice state, this is my owners fault ${Bot.sadEmoji.asMention}")
        val channel = state.channel?: return CommandResult.fail("i cant do that unless youre in a voice channel ${Bot.sadEmoji.asMention}")
        Bot.getGuildAudioPlayer(event.guild).play(channel, URL(args[0]))

        return CommandResult.success()
    }
}