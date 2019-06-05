package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.listeners.VoiceListener
import me.fzzy.robofzzy4j.util.CommandCost
import me.fzzy.robofzzy4j.util.CommandResult
import sx.blah.discord.Discord4J
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IVoiceChannel
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader


object Play : Command {

    override val cooldownMillis: Long = 1000 * 60 * 10
    override val votes: Boolean = true
    override val description = "Plays audio in the voice channel"
    override val usageText: String = "play <videoUrl>"
    override val allowDM: Boolean = true
    override val price: Int = 4
    override val cost: CommandCost = CommandCost.CURRENCY

    override fun runCommand(message: IMessage, args: List<String>): CommandResult {

        val userVoiceChannel = message.author.getVoiceStateForGuild(message.guild).channel
                ?: return CommandResult.fail("i cant do that unless youre in a voice channel")
        if (args.isEmpty()) return CommandResult.fail("thats not how you use that command $usageText")

        return play(userVoiceChannel, args[0], message.longID)
    }

    fun play(channel: IVoiceChannel, url: String, messageId: Long = 0, playTimeSeconds: Int = 60, playTimeAdjustment: Int = 40): CommandResult {
        val currentTime = System.currentTimeMillis()

        Discord4J.LOGGER.info("attempting to get media from $url")

        val rt = Runtime.getRuntime()
        val process = rt.exec("youtube-dl -x --audio-format mp3 --no-playlist $url -o \"cache${File.separator}$currentTime.%(ext)s\"")
        val stdInput = BufferedReader(InputStreamReader(process.inputStream))
        val stdError = BufferedReader(InputStreamReader(process.errorStream))

        var s: String?
        do {
            s = stdInput.readLine()
            if (s != null)
                println(s)
        } while (s != null)

        do {
            s = stdError.readLine()
            if (s != null)
                println(s)
        } while (s != null)

        val file = File("cache${File.separator}$currentTime.mp3")
        if (!file.exists()) return CommandResult.fail("sorry i couldnt get any media from that url")

        VoiceListener.playTempAudio(channel, file, true, 0.25F, playTimeSeconds, playTimeAdjustment, messageId)
                ?: return CommandResult.fail("i couldnt play that audio for some reason")

        return CommandResult.success()
    }
}