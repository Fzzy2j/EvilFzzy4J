package me.fzzy.evilfzzy4j.voice

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.VoiceChannel
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.LinkedBlockingQueue

class FzzyPlayer constructor(val guild: Guild) : AudioEventAdapter() {

    val player: AudioPlayer = Bot.playerManager.createPlayer()
    val queue = LinkedBlockingQueue<AudioTrack>()

    init {
        guild.audioManager.sendingHandler = AudioPlayerSendHandler(player)
        player.addListener(this)
    }

    fun play(channel: VoiceChannel, url: URL): CommandResult {
        if (channel.guild.id != guild.id) return CommandResult.fail("trying to play audio in a different guild than the audio player?")

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

        if (!file.exists()) return CommandResult.fail("i couldnt get media from that url ${Bot.surprisedEmote}")

        play(channel, file)

        return CommandResult.success()
    }

    fun play(channel: VoiceChannel, file: File): CommandResult {
        Bot.playerManager.loadItem(file.path, object : AudioLoadResultHandler {
            override fun loadFailed(exception: FriendlyException?) {
                Bot.logger.error("load failed")
            }

            override fun trackLoaded(track: AudioTrack) {
                Bot.logger.info("track loaded")
                channel.guild.audioManager.openAudioConnection(channel)
                queue.add(track)

                if (player.playingTrack == null) {
                    player.playTrack(queue.poll())
                }
            }

            override fun noMatches() {
                Bot.logger.error("no matches")
            }

            override fun playlistLoaded(playlist: AudioPlaylist) {
                var firstTrack = playlist.selectedTrack

                if (firstTrack == null) {
                    firstTrack = playlist.tracks[0]
                }

                queue.add(firstTrack)
            }

        })

        return CommandResult.success()
    }

    override fun onTrackStart(player: AudioPlayer?, track: AudioTrack?) {
        player?.volume = 50
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack?, endReason: AudioTrackEndReason?) {
        if (queue.isEmpty()) {
            guild.audioManager.closeAudioConnection()
        } else {
            player.playTrack(queue.poll())
        }
    }

}