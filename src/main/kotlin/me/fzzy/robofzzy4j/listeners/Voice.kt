package me.fzzy.robofzzy4j.listeners

import com.sedmelluq.discord.lavaplayer.container.mp3.Mp3AudioTrack
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import discord4j.core.`object`.entity.VoiceChannel
import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.FzzyGuild
import java.io.File
import java.util.*

object Voice {

    var interupt = false

    private const val MESSAGE_DELETE_DELAY_SECONDS = 60

    fun playTempAudio(channel: VoiceChannel, file: File, delete: Boolean, volume: Float = 1F, playTimeSeconds: Int = 0, playTimeAdjustment: Int = 0, messageId: Long = 0): UUID? {
        if (!interupt) {


            val ch = channel.guild.block()!!.getMemberById(Bot.client.selfId.get()).block()!!.voiceState.block()!!.channel.block()
            val audioP = FzzyGuild.getGuild(channel.guild.block()!!).player
            if (!file.exists()) return null
            if (ch == null && audioP.player.playingTrack == null)
                channel.join { spec -> spec.setProvider(audioP) }


            try {
                val id = UUID.randomUUID()
                val track = audioP.player.playTrack(Mp3AudioTrack(AudioTrackInfo()))
                track.metadata["fzzyChannel"] = channel.longID
                track.metadata["fzzyVolume"] = volume
                track.metadata["fzzyId"] = id
                track.metadata["fzzyTimeSeconds"] = playTimeSeconds
                track.metadata["fzzyTimeAdjustment"] = playTimeAdjustment
                track.metadata["fzzyMessageId"] = messageId
                return id
            } catch (e: Exception) {
                e.printStackTrace()
                Schedulerasdf.Builder(2).doAction { channel.leave() }.execute()
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
            Schedulerasdf.Builder(MESSAGE_DELETE_DELAY_SECONDS).doAction {
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
            Schedulerasdf.Builder(MESSAGE_DELETE_DELAY_SECONDS).doAction {
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