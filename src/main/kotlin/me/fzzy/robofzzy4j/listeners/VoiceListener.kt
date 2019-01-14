package me.fzzy.robofzzy4j.listeners

import me.fzzy.robofzzy4j.RoboFzzy
import me.fzzy.robofzzy4j.thread.IndividualTask
import me.fzzy.robofzzy4j.thread.Task
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.audio.AudioPlayer
import sx.blah.discord.util.audio.events.TrackFinishEvent
import sx.blah.discord.util.audio.events.TrackSkipEvent
import sx.blah.discord.util.audio.events.TrackStartEvent
import java.io.File
import java.io.IOException
import java.lang.NullPointerException
import java.util.*
import javax.sound.sampled.UnsupportedAudioFileException

object VoiceListener {

    var interupt = false

    const val MESSAGE_DELETE_DELAY = 8

    fun playTempAudio(channel: IVoiceChannel, file: File, delete: Boolean, volume: Float = 1F, playTimeSeconds: Int = 0, playTimeAdjustment: Int = 0, messageId: Long = 0): UUID? {
        if (!interupt) {
            val audioP = AudioPlayer.getAudioPlayerForGuild(channel.guild)
            if (!file.exists()) return null
            if (!channel.connectedUsers.contains(RoboFzzy.cli.ourUser) && audioP.currentTrack == null)
                channel.join()

            try {
                val id = UUID.randomUUID()
                val track = audioP.queue(file)
                track.metadata["fzzyChannel"] = channel.longID
                track.metadata["fzzyVolume"] = volume
                track.metadata["fzzyId"] = id
                track.metadata["fzzyTimeSeconds"] = playTimeSeconds
                track.metadata["fzzyTimeAdjustment"] = playTimeAdjustment
                track.metadata["fzzyMessageId"] = messageId
                return id
            } catch (e: Exception) {
                e.printStackTrace()
                Task.registerTask(IndividualTask({ channel.leave() }, 2, false))
            }
            if (delete)
                file.delete()
        }
        return null
    }

    fun getId(track: AudioPlayer.Track): UUID? {
        return track.metadata["fzzyId"] as UUID
    }

    val overrides = arrayListOf<Long>()

    @EventSubscriber
    fun onTrackBegin(event: TrackStartEvent) {
        if (event.track.metadata.containsKey("fzzyVolume"))
            event.player.volume = event.track.metadata["fzzyVolume"] as Float
        if (event.track.metadata.containsKey("fzzyChannel")) {
            val channel = RoboFzzy.cli.getVoiceChannelByID(event.track.metadata["fzzyChannel"] as Long)
            if (!channel.connectedUsers.contains(RoboFzzy.cli.ourUser))
                RoboFzzy.cli.ourUser.moveToVoiceChannel(channel)
        }
        if (event.track.metadata.containsKey("fzzyTimeSeconds")) {
            if (event.track.metadata["fzzyTimeSeconds"] as Int > 0) {
                val startTime = System.currentTimeMillis()
                Thread {
                    while (true) {
                        Thread.sleep(1000)
                        val message = RoboFzzy.cli.getMessageByID(event.track.metadata["fzzyMessageId"] as Long)

                        if (message != null) {
                            if (overrides.contains(message.longID)) {
                                overrides.remove(message.longID)
                                break
                            }
                        }

                        val voteAdjust = try {
                            VoteListener.getVotes(message) * event.track.metadata["fzzyTimeAdjustment"] as Int
                        } catch (e: Exception) {
                            -60 * 200
                        }

                        if (System.currentTimeMillis() - startTime > (event.track.metadata["fzzyTimeSeconds"] as Int + voteAdjust) * 1000) {
                            val track = event.player.currentTrack
                            if (track != null && VoiceListener.getId(track) == getId(event.track))
                                event.player.skip()
                            break
                        }
                    }
                }.start()
            }
        }
    }

    @EventSubscriber
    fun onTrackSkip(event: TrackSkipEvent) {
        val message = RoboFzzy.cli.getMessageByID(event.track.metadata["fzzyMessageId"] as Long)
        try {
            Task.registerTask(IndividualTask({
                RequestBuffer.request { message.delete() }
            }, MESSAGE_DELETE_DELAY, false))
        } catch(e: MissingPermissionsException) {
        }
        if (event.nextTrack == null && !interupt) {
            RoboFzzy.cli.ourUser.getVoiceStateForGuild(event.player.guild).channel?.leave()
        }
    }

    @EventSubscriber
    fun onTrackEnd(event: TrackFinishEvent) {
        val message = RoboFzzy.cli.getMessageByID(event.oldTrack.metadata["fzzyMessageId"] as Long)
        try {
            Task.registerTask(IndividualTask({
                RequestBuffer.request { message.delete() }
            }, MESSAGE_DELETE_DELAY, false))
        } catch(e: MissingPermissionsException) {
        }
        if (!event.newTrack.isPresent && !interupt) {
            RoboFzzy.cli.ourUser.getVoiceStateForGuild(event.player.guild).channel?.leave()
        }
    }

}