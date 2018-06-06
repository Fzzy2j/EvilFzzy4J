package me.fzzy.eventvoter

import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.audio.AudioPlayer
import java.io.File
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException

fun sendMessage(channel: IChannel, text: String): IMessage? {
    if (text.isEmpty())
        return null
    return try {
        channel.sendMessage(text)
    } catch (e: MissingPermissionsException) {
        null
    }
}



class TempMessage constructor(private var timeToStayMillis: Long, private var msg: IMessage) : Thread() {
    override fun run() {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeToStayMillis) {
            Thread.sleep(100L)
        }
        RequestBuffer.request { msg.delete() }
    }
}

class Sound constructor(private var userVoiceChannel: IVoiceChannel, private var audioP: AudioPlayer, private var audioDir: File, private var guild: IGuild) : Thread() {

    override fun run() {
        userVoiceChannel.join()
        Thread.sleep(100)
        audioP.clear()
        Thread.sleep(100)
        try {
            audioP.queue(audioDir)
        } catch (e: IOException) {
            // File not found
        } catch (e: UnsupportedAudioFileException) {
            e.printStackTrace()
        }
        while (audioP.currentTrack != null) {
            Thread.sleep(100)
        }
        cli.ourUser.getVoiceStateForGuild(guild).channel?.leave()
    }
}