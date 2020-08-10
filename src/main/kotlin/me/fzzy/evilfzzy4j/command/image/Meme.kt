package me.fzzy.evilfzzy4j.command.image

import ch.qos.logback.core.util.FileUtil
import com.google.auth.oauth2.ServiceAccountJwtAccessCredentials
import com.google.cloud.texttospeech.v1.*
import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import me.fzzy.evilfzzy4j.voice.FzzyPlayer
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.apache.commons.io.FileUtils
import org.im4java.core.IMOperation
import org.im4java.core.ImageMagickCmd
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.collections.ArrayList

object Meme : Command("meme") {

    override val cooldownMillis: Long = 10 * 1000
    override val description = "Puts meme text onto an image"
    override val args: ArrayList<String> = arrayListOf("text")
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {

        val full = args.joinToString(" ").replace("'", "").replace("\n", "").split("|")

        val topText = full[0]
        var bottomText: String? = null
        if (full.size > 1) bottomText = full[1]

        val file = Bot.getRecentImage(event.channel, latestMessageId)
                ?: return CommandResult.fail("i couldnt get an image file ${Bot.sadEmote.asMention}")

        val convert = ImageMagickCmd("convert")
        val operation = IMOperation()

        operation.addImage(file.absolutePath)

        operation.fill("white")
        operation.font("Impact")
        operation.stroke("black")
        operation.strokewidth(2)

        if (topText.isNotBlank())
            annotateCenter(file, operation, topText.toUpperCase(), false)

        if (bottomText != null)
            annotateCenter(file, operation, bottomText.toUpperCase(), true)

        operation.addImage(file.absolutePath)

        convert.run(operation)

        event.textChannel.sendFile(file).queue()
        file.delete()

        val guild = event.guild
        val voiceState = guild.getMember(event.author)?.voiceState
        if (voiceState != null) {
            val channel = voiceState.channel
            if (channel != null) {
                var tts = topText
                if (bottomText != null) tts += bottomText
                val speech = getTextToSpeech("$tts")
                if (speech != null) {
                    val sound = File("cache/${System.currentTimeMillis()}.mp3")
                    FileUtils.writeByteArrayToFile(sound, speech)
                    val player = FzzyPlayer.getPlayer(guild)
                    val volume = if (Random().nextInt(10) > 7 || tts.toUpperCase() == tts) 10000 else 100
                    player.play(channel, sound.path, volume)
                }
            }
        }

        return CommandResult.success()
    }

    fun annotateCenter(imgFile: File, operation: IMOperation, text: String, bottom: Boolean) {
        val img = ImageIO.read(imgFile)
        val affine = AffineTransform()
        val frc = FontRenderContext(affine, true, true)
        var pointSize = 1
        var font = Font("Impact", Font.PLAIN, pointSize)
        var textWidth = font.getStringBounds(text, frc).width
        while (textWidth < img.width * 0.75) {
            pointSize += 1
            font = Font("Impact", Font.PLAIN, pointSize)
            textWidth = font.getStringBounds(text, frc).width
        }

        if (bottom)
            operation.gravity("south")
        else
            operation.gravity("north")

        operation.pointsize(pointSize)
        operation.draw("\"text 0,20 '$text'\"")
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
            tts.shutdownNow()
            content.toByteArray()
        } catch (e: Exception) {
            null
        }
    }
}