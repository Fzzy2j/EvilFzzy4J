package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.CommandResult
import me.fzzy.robofzzy4j.Funcs
import me.fzzy.robofzzy4j.listeners.VoiceListener
import org.apache.commons.io.FileUtils
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.audio.AudioPlayer
import java.io.File
import java.io.IOException
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.UnsupportedAudioFileException

object Tts : Command {

    override val cooldownCategory = "audio"
    override val cooldownMillis: Long = 60 * 10 * 1000
    override val votes: Boolean = true
    override val description = "Joins the voice channel and plays text to speech"
    override val usageText: String = "tts <text>"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        if (args.isNotEmpty()) {
            var text = ""
            for (arg in args) {
                text += " $arg"
            }
            text = text.substring(1)
            val fileName = "cache/${System.currentTimeMillis()}.mp3"
            val speech = Funcs.getTextToSpeech(text) ?: return CommandResult.fail("the text to speech api didnt work for some reason")
            val sound = File(fileName)
            FileUtils.writeByteArrayToFile(sound, speech)
            val userVoiceChannel = event.author.getVoiceStateForGuild(event.guild).channel
            if (userVoiceChannel != null) {
                if (VoiceListener.playTempAudio(userVoiceChannel, sound, true, 1F, 40, 20, event.messageID) == null)
                    return CommandResult.fail("im sorry, something went wrong when i tried to do that")
            } else
                return CommandResult.fail("i cant do that if youre not in a voice channel")
        } else
            return CommandResult.fail("umm what? $usageText")
        return CommandResult.success()
    }
}