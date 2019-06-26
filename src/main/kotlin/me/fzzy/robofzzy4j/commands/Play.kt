package me.fzzy.robofzzy4j.commands

import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.VoiceChannel
import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.util.CommandCost
import me.fzzy.robofzzy4j.util.CommandResult
import reactor.core.publisher.Mono
import java.io.File


object Play : Command("play") {

    override val cooldownMillis: Long = 1000 * 60 * 10
    override val votes: Boolean = true
    override val description = "Plays audio in the voice channel"
    override val args: ArrayList<String> = arrayListOf("url")
    override val allowDM: Boolean = true
    override val price: Int = 4
    override val cost: CommandCost = CommandCost.CURRENCY

    override fun runCommand(message: Message, args: List<String>): Mono<CommandResult> {

        //val userVoiceChannel = message.author.getVoiceStateForGuild(message.guild).channel
                //?: return CommandResult.fail("i cant do that unless youre in a voice channel ${Bot.HAPPY_EMOJI}")

        //return play(userVoiceChannel, args[0], message.longID)
        return Mono.just(CommandResult.fail("this command is not implemented in this version of me ${Bot.toUsable(Bot.sadEmoji)}"))
    }

    fun play(channel: VoiceChannel, url: String, messageId: Long = 0, playTimeSeconds: Int = 60, playTimeAdjustment: Int = 40): CommandResult {

        val currentTime = System.currentTimeMillis()

        Bot.logger.info("attempting to get media from $url")

        val rt = Runtime.getRuntime()
        val process = rt.exec("youtube-dl -x --audio-format mp3 --no-playlist $url -o \"cache${File.separator}$currentTime.%(ext)s\"")
        process.waitFor()

        val file = File("cache${File.separator}$currentTime.mp3")

        if (!file.exists())
            return CommandResult.fail("i couldnt get media from that url ${Bot.toUsable(Bot.surprisedEmoji)}")
        //Voice.playTempAudio(channel, file, true, 0.25F, playTimeSeconds, playTimeAdjustment, messageId)

        return CommandResult.success()
    }
}