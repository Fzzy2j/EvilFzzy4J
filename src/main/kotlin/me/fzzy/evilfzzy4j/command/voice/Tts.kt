package me.fzzy.evilfzzy4j.command.voice

import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials
import com.google.cloud.texttospeech.v1.*
import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.apache.commons.io.FileUtils
import java.io.File


object Tts : Command("tts") {

    override val cooldownMillis: Long = 60 * 10 * 1000
    override val description = "Joins the voice channel and plays text to speech"
    override val args: ArrayList<String> = arrayListOf("text")
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {
        if (args.isNotEmpty()) {
            var text = ""
            for (arg in args) {
                text += " $arg"
            }
            text = text.substring(1)
            val fileName = "cache/${System.currentTimeMillis()}.mp3"
            val speech = getTextToSpeech(text) ?: return CommandResult.fail("the text to speech api didnt work ${Bot.sadEmoji.asMention}")
            val sound = File(fileName)
            FileUtils.writeByteArrayToFile(sound, speech)
            val state = event.member!!.voiceState?: return CommandResult.fail("i cant get your voice state, this is my owners fault ${Bot.sadEmoji.asMention}")
            val channel = state.channel?: return CommandResult.fail("i cant do that unless youre in a voice channel ${Bot.sadEmoji.asMention}")
            Bot.getGuildAudioPlayer(event.guild).play(channel, sound)

            return CommandResult.success()
        }
        return CommandResult.fail("i dont know what you want ${Bot.sadEmoji.asMention}")
    }

    fun getTextToSpeech(text: String): ByteArray? {
        val voices = arrayListOf(
                "en-US-Wavenet-A",
                "en-US-Wavenet-B",
                "en-US-Wavenet-C",
                "en-US-Wavenet-D",
                "en-US-Wavenet-E",
                "en-US-Wavenet-F"
        )
        return try {
            val settings = TextToSpeechSettings.newBuilder().setCredentialsProvider { ServiceAccountJwtAccessCredentials.fromStream(Bot.TTS_AUTH_FILE.inputStream()) }.build()
            val tts = TextToSpeechClient.create(settings)

            val input = SynthesisInput.newBuilder().setText(text).build()
            val voice = VoiceSelectionParams.newBuilder().setLanguageCode("en-US").setName(voices[Bot.random.nextInt(voices.count())]).build()
            val config = AudioConfig.newBuilder().setAudioEncoding(AudioEncoding.MP3).build()
            val response = tts.synthesizeSpeech(input, voice, config)
            val content = response.audioContent
            content.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}