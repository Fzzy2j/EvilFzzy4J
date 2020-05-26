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
import me.fzzy.evilfzzy4j.command.image.Fzzy
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.VoiceChannel
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.net.URL
import java.util.concurrent.LinkedBlockingQueue

class FzzyPlayer private constructor(val guild: Guild) : AudioEventAdapter() {

    companion object {
        private val players = hashMapOf<String, FzzyPlayer>()

        fun getPlayer(guild: Guild): FzzyPlayer {
            if (players.containsKey(guild.id)) return players[guild.id]!!
            val player = FzzyPlayer(guild)
            players[guild.id] = player
            return player
        }
    }

    val player: AudioPlayer = Bot.playerManager.createPlayer()
    val queue = LinkedBlockingQueue<AudioTrack>()

    init {
        guild.audioManager.sendingHandler = AudioPlayerSendHandler(player)
        player.addListener(this)
    }

    fun play(channel: VoiceChannel, identifier: String) {
        Bot.playerManager.loadItem(identifier, object : AudioLoadResultHandler {
            override fun loadFailed(exception: FriendlyException?) {
                Bot.logger.error("load failed")
            }

            override fun trackLoaded(track: AudioTrack) {
                Bot.logger.info("track loaded")
                queue.add(track)

                if (player.playingTrack == null) {
                    channel.guild.audioManager.openAudioConnection(channel)
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