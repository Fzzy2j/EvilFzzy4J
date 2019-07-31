package me.fzzy.evilfzzy4j.command.voice

import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials
import com.google.cloud.texttospeech.v1.*
import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandCost
import me.fzzy.evilfzzy4j.command.CommandResult
import org.apache.commons.io.FileUtils
import reactor.core.publisher.Mono
import java.io.File


object Tts : Command("tts") {

    override val cooldownMillis: Long = 60 * 10 * 1000
    override val votes: Boolean = true
    override val description = "Joins the voice channel and plays text to speech"
    override val args: ArrayList<String> = arrayListOf("text")
    override val allowDM: Boolean = true
    override val price: Int = 4
    override val cost: CommandCost = CommandCost.CURRENCY

    override fun runCommand(message: CachedMessage, args: List<String>): Mono<CommandResult> {
        if (args.isNotEmpty()) {
            var text = ""
            for (arg in args) {
                text += " $arg"
            }
            text = text.substring(1)
            val fileName = "cache/${System.currentTimeMillis()}.mp3"
            val speech = getTextToSpeech(text) ?: return Mono.just(CommandResult.fail("the text to speech api didnt work ${Bot.sadEmoji()}"))
            val sound = File(fileName)
            FileUtils.writeByteArrayToFile(sound, speech)
            val guild = FzzyGuild.getGuild(message.guild.id)
            guild.player.play(message.authorAsMember.voiceState.block()!!.channel.block()!!, sound, message.original!!.id)

            return Mono.just(CommandResult.success())
        }
        return Mono.just(CommandResult.fail("i dont know what you want ${Bot.sadEmoji()}"))
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