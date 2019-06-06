package me.fzzy.robofzzy4j.listeners

import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.thread.Scheduler
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.audio.AudioPlayer
import sx.blah.discord.util.audio.events.TrackFinishEvent
import sx.blah.discord.util.audio.events.TrackSkipEvent
import sx.blah.discord.util.audio.events.TrackStartEvent
import java.io.File
import java.util.*

object VoiceListener {

    var interupt = false

    private const val MESSAGE_DELETE_DELAY_SECONDS = 60

    fun playTempAudio(channel: IVoiceChannel, file: File, delete: Boolean, volume: Float = 1F, playTimeSeconds: Int = 0, playTimeAdjustment: Int = 0, messageId: Long = 0): UUID? {
        if (!interupt) {
            val audioP = AudioPlayer.getAudioPlayerForGuild(channel.guild)
            if (!file.exists()) return null
            if (!channel.connectedUsers.contains(Bot.client.ourUser) && audioP.currentTrack == null)
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
                Scheduler.Builder(2).doAction { channel.leave() }.execute()
            }
            if (delete)
                file.delete()
        }
        return null
    }

    fun getId(track: AudioPlayer.Track): UUID? {
        return track.metadata["fzzyId"] as UUID
    }

    @EventSubscriber
    fun onTrackBegin(event: TrackStartEvent) {
        if (event.track.metadata.containsKey("fzzyVolume"))
            event.player.volume = event.track.metadata["fzzyVolume"] as Float
        if (event.track.metadata.containsKey("fzzyChannel")) {
            val channel = Bot.client.getVoiceChannelByID(event.track.metadata["fzzyChannel"] as Long)
            if (!channel.connectedUsers.contains(Bot.client.ourUser))
                Bot.client.ourUser.moveToVoiceChannel(channel)
        }
        if (event.track.metadata.containsKey("fzzyTimeSeconds")) {
            if (event.track.metadata["fzzyTimeSeconds"] as Int > 0) {
                val startTime = System.currentTimeMillis()
                Thread {
                    while (true) {
                        Thread.sleep(1000)
                        val messageId = event.track.metadata["fzzyMessageId"] as Long
                        if (messageId == 0L) break
                        val message = Bot.client.getMessageByID(messageId)

                        if (message == null) {
                            event.player.skip()
                        } else {
                            val voteAdjust = VoteListener.getVotes(message) * event.track.metadata["fzzyTimeAdjustment"] as Int

                            if (System.currentTimeMillis() - startTime > (event.track.metadata["fzzyTimeSeconds"] as Int + voteAdjust) * 1000) {
                                val track = event.player.currentTrack
                                if (track != null && getId(track) == getId(event.track))
                                    event.player.skip()
                                break
                            }
                        }
                    }
                }.start()
            }
        }
    }

    @EventSubscriber
    fun onTrackSkip(event: TrackSkipEvent) {
        val message = Bot.client.getMessageByID(event.track.metadata["fzzyMessageId"] as Long)
        try {
            Scheduler.Builder(MESSAGE_DELETE_DELAY_SECONDS).doAction {
                RequestBuffer.request {
                    try {
                        message.delete()
                    } catch (e: Exception) {
                    }
                }
            }.execute()
        } catch (e: MissingPermissionsException) {
        }
        if (event.nextTrack == null && !interupt) {
            Bot.client.ourUser.getVoiceStateForGuild(event.player.guild).channel?.leave()
        }
    }

    @EventSubscriber
    fun onTrackEnd(event: TrackFinishEvent) {
        val message = Bot.client.getMessageByID(event.oldTrack.metadata["fzzyMessageId"] as Long)
        try {
            Scheduler.Builder(MESSAGE_DELETE_DELAY_SECONDS).doAction {
                RequestBuffer.request {
                    try {
                        message.delete()
                    } catch (e: Exception) {
                    }
                }
            }.execute()
        } catch (e: MissingPermissionsException) {
        }
        if (!event.newTrack.isPresent && !interupt) {
            Bot.client.ourUser.getVoiceStateForGuild(event.player.guild).channel?.leave()
        }
    }

}