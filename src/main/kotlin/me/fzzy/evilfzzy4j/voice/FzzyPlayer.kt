package me.fzzy.evilfzzy4j.voice

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import discord4j.core.`object`.entity.VoiceChannel
import discord4j.core.`object`.util.Snowflake
import discord4j.voice.VoiceConnection
import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.command.CommandResult
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL

class FzzyPlayer constructor(val provider: LavaPlayerAudioProvider) : AudioLoadResultHandler, AudioEventAdapter() {

    val queue = arrayListOf<AudioTrack>()

    init {
        provider.player.addListener(this)
    }

    var connection: VoiceConnection? = null

    fun play(channel: VoiceChannel, url: URL, messageId: Snowflake? = null): CommandResult {

        val currentTime = System.currentTimeMillis()

        Bot.logger.info("attempting to get media from $url")

        val rt = Runtime.getRuntime()
        val process = rt.exec("youtube-dl -x --audio-format mp3 --no-playlist $url -o \"cache${File.separator}$currentTime.%(ext)s\"")
        val stdInput = BufferedReader(InputStreamReader(process.inputStream))
        val stdError = BufferedReader(InputStreamReader(process.errorStream))

        var input = stdInput.readLine()
        while (input != null) {
            Bot.logger.info(input)
            input = stdInput.readLine()
        }

        var error = stdError.readLine()
        while (error != null) {
            Bot.logger.info(error)
            error = stdError.readLine()
        }

        val file = File("cache${File.separator}$currentTime.mp3")

        if (!file.exists())
            return CommandResult.fail("i couldnt get media from that url ${Bot.surprisedEmoji()}")

        play(channel, file, messageId)

        return CommandResult.success()
    }

    fun play(channel: VoiceChannel, file: File, messageId: Snowflake? = null): CommandResult {
        if (connection == null) connection = channel.join { spec -> spec.setProvider(provider) }.block()
        Bot.playerManager.loadItem(file.path, this)

        return CommandResult.success()
    }

    override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
        player?.volume = 50
    }

    override fun onTrackEnd(player: AudioPlayer?, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        if (queue.count() == 0) {
            connection?.disconnect()
            connection = null
        } else {
            provider.player.playTrack(queue[0])
            queue.removeAt(0)
        }
    }

    override fun trackLoaded(track: AudioTrack) {
        // LavaPlayer found an audio source for us to play
        Bot.logger.info("track loaded")
        if (provider.player.playingTrack == null) {
            provider.player.playTrack(track)
        } else
            queue.add(track)
    }

    override fun playlistLoaded(playlist: AudioPlaylist) {
        // LavaPlayer found multiple AudioTracks from some playlist
    }

    override fun noMatches() {
        Bot.logger.error("no matches")
        // LavaPlayer did not find any audio to extract
    }

    override fun loadFailed(exception: FriendlyException) {
        Bot.logger.error("load failed")
        // LavaPlayer could not parse an audio source for some reason
    }

}