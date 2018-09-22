package me.fzzy.robofzzy4j.listeners

import me.fzzy.robofzzy4j.cli
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.util.audio.AudioPlayer
import sx.blah.discord.util.audio.events.TrackFinishEvent
import java.io.File
import java.io.IOException
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException

class VoiceListener {

    companion object {
        private var tempFile: File? = null
        private var delete = true

        fun playTempAudio(channel: IVoiceChannel, file: File, delete: Boolean) {
            tempFile = file
            this.delete = delete

            val audioP = AudioPlayer.getAudioPlayerForGuild(channel.guild)

            channel.join()
            audioP.clear()
            try {
                audioP.queue(file)
            } catch (e: IOException) {
                // File not found
            } catch (e: UnsupportedAudioFileException) {
                e.printStackTrace()
            }
        }
    }

    @EventSubscriber
    fun onTrackEnd(e: TrackFinishEvent) {
        if (!e.newTrack.isPresent) {
            if (delete)
                tempFile?.delete()
            cli.ourUser.getVoiceStateForGuild(e.player.guild).channel?.leave()
        }
    }

}