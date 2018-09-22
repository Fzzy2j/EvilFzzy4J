package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.Funcs
import me.fzzy.robofzzy4j.cli
import me.fzzy.robofzzy4j.listeners.VoiceListener
import org.apache.commons.io.FileUtils
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.audio.AudioPlayer
import java.io.File
import java.io.IOException
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException

class Tts : Command {

    override val cooldownMillis: Long = 60 * 5 * 1000
    override val attemptDelete: Boolean = true
    override val description = "Joins the voice channel and plays text to speech"
    override val usageText: String = "-tts <text>"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        if (args.isNotEmpty()) {
            var text = ""
            for (arg in args) {
                text += " $arg"
            }
            text = text.substring(1)
            Thread {
                val fileName = "${System.currentTimeMillis()}.mp3"
                val speech = Funcs.getTextToSpeech(text)
                FileUtils.writeByteArrayToFile(File(fileName), speech)
                val sound = File(fileName)
                val userVoiceChannel = event.author.getVoiceStateForGuild(event.guild).channel
                if (userVoiceChannel != null) {
                    VoiceListener.playTempAudio(userVoiceChannel, sound, true)
                }
            }.start()
        }
    }
}