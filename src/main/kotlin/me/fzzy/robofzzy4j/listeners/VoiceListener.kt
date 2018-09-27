package me.fzzy.robofzzy4j.listeners

import me.fzzy.robofzzy4j.cli
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.util.audio.AudioPlayer
import sx.blah.discord.util.audio.events.TrackFinishEvent
import sx.blah.discord.util.audio.events.TrackSkipEvent
import sx.blah.discord.util.audio.events.TrackStartEvent
import java.io.File
import java.io.IOException
import java.util.*
import javax.sound.sampled.UnsupportedAudioFileException

class VoiceListener {

    private class QueueInstance constructor(var channel: IVoiceChannel, var file: File, var delete: Boolean, var volume: Float)

    companion object {

        var interupt = false

        fun playTempAudio(channel: IVoiceChannel, file: File, delete: Boolean, volume: Float = 1F, playTimeSeconds: Int = 0): UUID? {
            if (!interupt) {
                val audioP = AudioPlayer.getAudioPlayerForGuild(channel.guild)
                if (!channel.connectedUsers.contains(cli.ourUser) && audioP.currentTrack == null)
                    channel.join()

                try {
                    val id = UUID.randomUUID()
                    val track = audioP.queue(file)
                    track.metadata["fzzyChannel"] = channel.longID
                    track.metadata["fzzyVolume"] = volume
                    track.metadata["fzzyId"] = id
                    track.metadata["fzzyTimeSeconds"] = playTimeSeconds
                    return id
                } catch (e: IOException) {
                    // File not found
                } catch (e: UnsupportedAudioFileException) {
                    e.printStackTrace()
                }
                if (delete)
                    file.delete()
            }
            return null
        }

        fun getId(track: AudioPlayer.Track): UUID? {
            return track.metadata["fzzyId"] as UUID
        }
    }

    @EventSubscriber
    fun onTrackBegin(event: TrackStartEvent) {
        if (event.track.metadata.containsKey("fzzyVolume"))
            event.player.volume = event.track.metadata["fzzyVolume"] as Float
        if (event.track.metadata.containsKey("fzzyChannel")) {
            val channel = cli.getVoiceChannelByID(event.track.metadata["fzzyChannel"] as Long)
            if (!channel.connectedUsers.contains(cli.ourUser))
                cli.ourUser.moveToVoiceChannel(channel)
        }
        if (event.track.metadata.containsKey("fzzyTimeSeconds")) {
            if (event.track.metadata["fzzyTimeSeconds"] as Int > 0) {
                Thread {
                    Thread.sleep((event.track.metadata["fzzyTimeSeconds"] as Int).toLong() * 1000)
                    val track = event.player.currentTrack
                    if (VoiceListener.getId(track) == getId(event.track))
                        event.player.skip()
                }.start()
            }
        }
    }

    @EventSubscriber
    fun onTrackSkip(event: TrackSkipEvent) {
        if (event.nextTrack == null && !interupt) {
            cli.ourUser.getVoiceStateForGuild(event.player.guild).channel?.leave()
        }
    }

    @EventSubscriber
    fun onTrackEnd(event: TrackFinishEvent) {
        if (!event.newTrack.isPresent && !interupt) {
            cli.ourUser.getVoiceStateForGuild(event.player.guild).channel?.leave()
        }
    }

}