package me.fzzy.robofzzy4j.commands

import com.commit451.youtubeextractor.YouTubeExtractor
import me.fzzy.robofzzy4j.*
import me.fzzy.robofzzy4j.listeners.VoiceListener
import me.fzzy.robofzzy4j.util.FFMPEGLocalLocator
import sx.blah.discord.Discord4J
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.util.audio.AudioPlayer
import ws.schild.jave.*
import java.io.IOException
import java.io.FileOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.regex.Pattern


object Play : Command {

    override val cooldownMillis: Long = 1000 * 60 * 10
    override val votes: Boolean = true
    override val description = "Plays audio in the voice channel"
    override val usageText: String = "-play <videoUrl>"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        val userVoiceChannel = event.author.getVoiceStateForGuild(event.guild).channel?: return CommandResult.fail("i cant do that unless youre in a voice channel")
        if (args.size != 1) return CommandResult.fail("thats not how you use that command $usageText")
        return play(userVoiceChannel, args[0], event.messageID)
    }

    fun play(channel: IVoiceChannel, videoUrl: String, messageId: Long): CommandResult {
        val matcher = Pattern.compile("#(?<=v=)[a-zA-Z0-9-]+(?=&)|(?<=v\\/)[^&\\n]+|(?<=v=)[^&\\n]+|(?<=youtu.be/)[^&\\n]+#").matcher(videoUrl)
        val id: String
        try {
            id = if (matcher.find()) matcher.group(0) else videoUrl.split(".be/")[1]
        } catch (e: Exception) {
            return CommandResult.fail("thats not how you use that command $usageText")
        }
        val extraction = YouTubeExtractor.Builder().build().extract(id).blockingGet()
        if (extraction.lengthSeconds!! <= 60 * 10) {
            for (stream in extraction.videoStreams) {
                if (stream.format == "v3GPP")
                    continue

                Discord4J.LOGGER.info("Playing video audio with id: $id")
                val outputFile = File("cache/${System.currentTimeMillis()}.mp4")

                try {
                    val url = URL(stream.url)
                    val con = url.openConnection() as HttpURLConnection
                    con.requestMethod = "GET"
                    con.connect()

                    if (!outputFile.exists()) {
                        outputFile.createNewFile()
                    }

                    val fos = FileOutputStream(outputFile)

                    val input = con.inputStream

                    val buffer = ByteArray(1024)
                    var len1 = input.read(buffer)
                    while (len1 != -1) {
                        fos.write(buffer, 0, len1)
                        len1 = input.read(buffer)
                    }
                    fos.close()
                    input.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

                val target = File("cache/${System.currentTimeMillis()}.mp3")
                val audio = AudioAttributes()
                audio.setCodec("libmp3lame")
                audio.setBitRate(128000)
                audio.setChannels(2)
                audio.setSamplingRate(44100)
                val attrs = EncodingAttributes()
                attrs.setFormat("mp3")
                attrs.setAudioAttributes(audio)
                val encoder = Encoder(FFMPEGLocalLocator())

                encoder.encode(MultimediaObject(outputFile, FFMPEGLocalLocator()), target, attrs)

                outputFile.delete()

                    VoiceListener.playTempAudio(channel, target, true, 0.3F, 60, 40, messageId)
                            ?: return CommandResult.fail("i couldnt play that audio for some reason")
                break
            }
        } else
            return CommandResult.fail("videos longer than 10 minutes take too long for me to download, so i dont allow them. sorry.")
        return CommandResult.success()
    }
}