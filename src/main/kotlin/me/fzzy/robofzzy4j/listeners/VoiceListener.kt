package me.fzzy.robofzzy4j.listeners

import me.fzzy.robofzzy4j.cli
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.util.audio.AudioPlayer
import sx.blah.discord.util.audio.events.TrackFinishEvent
import java.io.File
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException

class VoiceListener {

    // stop command
    companion object {
        private var tempFile: File? = null
        private var delete = true

        private var playing = false

        var interupt = false

        fun playTempAudio(channel: IVoiceChannel, file: File, delete: Boolean, volume: Float): Boolean {
            if (!interupt && !playing) {
                tempFile?.delete()
                tempFile = file
                this.delete = delete

                val audioP = AudioPlayer.getAudioPlayerForGuild(channel.guild)

                channel.join()
                audioP.clear()
                try {
                    audioP.queue(file)
                    playing = true
                    audioP.volume = volume
                    return true
                } catch (e: IOException) {
                    // File not found
                } catch (e: UnsupportedAudioFileException) {
                    e.printStackTrace()
                }
            }
            return false
        }
    }

    @EventSubscriber
    fun onTrackEnd(e: TrackFinishEvent) {
        if (!e.newTrack.isPresent && !interupt) {
            if (delete) {
                tempFile?.delete()
                tempFile = null
            }
            playing = false
            cli.ourUser.getVoiceStateForGuild(e.player.guild).channel?.leave()
        }
    }

}