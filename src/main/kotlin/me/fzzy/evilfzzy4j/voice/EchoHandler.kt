package me.fzzy.evilfzzy4j.voice

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame
import net.dv8tion.jda.api.audio.AudioReceiveHandler
import net.dv8tion.jda.api.audio.AudioSendHandler
import java.nio.ByteBuffer

class EchoHandler : AudioSendHandler, AudioReceiveHandler {
    private val audioPlayer: AudioPlayer
    private val buffer: ByteBuffer
    private val frame: MutableAudioFrame

    /**
     * @param audioPlayer Audio player to wrap.
     */
    constructor(audioPlayer: AudioPlayer) {
        this.audioPlayer = audioPlayer
        this.buffer = ByteBuffer.allocate(1024)
        this.frame = MutableAudioFrame()
        this.frame.setBuffer(buffer)
    }

    override fun canProvide(): Boolean {
        // returns true if audio was provided
        return audioPlayer.provide(frame)
    }

    override fun provide20MsAudio(): ByteBuffer? {
        // flip to make it a read buffer
        buffer.flip()
        return buffer
    }

    override fun isOpus(): Boolean {
        return true
    }
}