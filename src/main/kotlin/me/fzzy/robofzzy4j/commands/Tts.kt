package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.listeners.VoiceListener
import me.fzzy.robofzzy4j.util.CommandCost
import me.fzzy.robofzzy4j.util.CommandResult
import org.apache.commons.io.FileUtils
import sx.blah.discord.handle.obj.IMessage
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection

object Tts : Command {

    override val cooldownMillis: Long = 60 * 10 * 1000
    override val votes: Boolean = true
    override val description = "Joins the voice channel and plays text to speech"
    override val usageText: String = "tts <text>"
    override val allowDM: Boolean = true
    override val price: Int = 4
    override val cost: CommandCost = CommandCost.CURRENCY

    override fun runCommand(message: IMessage, args: List<String>): CommandResult {
        if (args.isNotEmpty()) {
            var text = ""
            for (arg in args) {
                text += " $arg"
            }
            text = text.substring(1)
            val fileName = "cache/${System.currentTimeMillis()}.mp3"
            val speech = getTextToSpeech(text) ?: return CommandResult.fail("the text to speech api didnt work for some reason")
            val sound = File(fileName)
            FileUtils.writeByteArrayToFile(sound, speech)
            val userVoiceChannel = message.author.getVoiceStateForGuild(message.guild).channel
            if (userVoiceChannel != null) {
                if (VoiceListener.playTempAudio(userVoiceChannel, sound, true, 1F, 40, 20, message.longID) == null)
                    return CommandResult.fail("im sorry, something went wrong when i tried to do that")
            } else
                return CommandResult.fail("i cant do that if youre not in a voice channel")
        } else
            return CommandResult.fail("umm what? $usageText")
        return CommandResult.success()
    }

    fun getTextToSpeech(text: String): ByteArray? {
        if (Bot.azureAuth == null) return null
        val voices = listOf(
                "(en-US, BenjaminRUS)",
                "(en-GB, HazelRUS)",
                "(en-US, ZiraRUS)",
                "(en-US, JessaRUS)",
                "(en-AU, Catherine)",
                "(en-AU, HayleyRUS)",
                "(en-CA, Linda)"
        )

        val apiurl = "https://speech.platform.bing.com/synthesize"
        val input = "<speak version='1.0' xml:lang='en-US'>" +
                "<voice xml:lang='en-US' name='Microsoft Server Speech Text to Speech Voice ${voices[Bot.random.nextInt(voices.size)]}'>" +
                text +
                "</voice>" +
                "</speak>"

        try {
            val url = URL(apiurl)
            val urlConnection = url.openConnection() as HttpsURLConnection
            urlConnection.doInput = true
            urlConnection.doOutput = true
            urlConnection.connectTimeout = 5000
            urlConnection.readTimeout = 15000
            urlConnection.requestMethod = "POST"
            urlConnection.setRequestProperty("Content-Type", "application/ssml+xml")
            urlConnection.setRequestProperty("X-MICROSOFT-OutputFormat", "audio-16khz-64kbitrate-mono-mp3")
            urlConnection.setRequestProperty("Authorization", "Bearer ${Bot.azureAuth!!.GetAccessToken()}")
            urlConnection.setRequestProperty("Accept", "*/*")
            val ssmlBytes = input.toByteArray()
            urlConnection.setRequestProperty("content-length", ssmlBytes.size.toString())
            urlConnection.connect()
            urlConnection.outputStream.write(ssmlBytes)
            val code = urlConnection.responseCode
            if (code == 200) {
                val `in` = urlConnection.inputStream
                val bout = ByteArrayOutputStream()
                val bytes = ByteArray(1024)
                var ret = `in`.read(bytes)
                while (ret > 0) {
                    bout.write(bytes, 0, ret)
                    ret = `in`.read(bytes)
                }
                return bout.toByteArray()
            } else
                println(code)
            urlConnection.disconnect()
        } catch (e: Exception) {
            return null
        }
        return null
    }
}