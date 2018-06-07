package me.fzzy.eventvoter

import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.json.JSONArray
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.audio.AudioPlayer
import java.io.*
import java.net.URL
import java.util.*
import java.util.regex.Pattern
import javax.sound.sampled.UnsupportedAudioFileException

class Funcs {
    companion object {
        fun sendMessage(channel: IChannel, text: String): IMessage? {
            if (text.isEmpty())
                return null
            return try {
                channel.sendMessage(text)
            } catch (e: MissingPermissionsException) {
                null
            }
        }

        fun sendFile(channel: IChannel, file: File): IMessage? {
            return try {
                channel.sendFile(file)
            } catch (e: MissingPermissionsException) {
                null
            }
        }
    }
}

class ImageFuncs {
    companion object {

        fun getMinecraftAchievement(text: String): URL {
            val url = "https://mcgen.herokuapp.com/a.php?i=${Random().nextInt(20) + 1}&h=Achievement+get!&t=$text"
            return URL(url)
        }

        fun downloadTempFile(url: URL): File? {
            val fileName = "${imageQueues++}.${if (url.toString().endsWith("webp") || url.toString().endsWith("png")) "png" else "jpg"}"
            try {
                val openConnection = url.openConnection()
                openConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
                openConnection.connect()

                val inputStream = BufferedInputStream(openConnection.getInputStream())
                val outputStream = BufferedOutputStream(FileOutputStream(fileName))

                for (out in inputStream.iterator()) {
                    outputStream.write(out.toInt())
                }
                inputStream.close()
                outputStream.close()
            } catch (e: Exception) {
                return null
            }
            if (imageQueues > 1000000)
                imageQueues = 0
            return File(fileName)
        }

        fun getFacialInfo(attributes: String, faceId: Boolean, faceLandmarks: Boolean, url: String): JSONArray? {
            val apiurl = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0/detect"
            val httpClient = HttpClientBuilder.create().build()
            try {
                val builder = URIBuilder(apiurl)

                builder.setParameter("returnFaceId", faceId.toString())
                builder.setParameter("returnFaceLandmarks", faceLandmarks.toString())
                builder.setParameter("returnFaceAttributes", attributes)

                val uri = builder.build()
                val request = HttpPost(uri)

                request.setHeader("Content-Type", "application/json")
                request.setHeader("Ocp-Apim-Subscription-Key", azureToken)

                val reqEntity = StringEntity("{\"url\":\"$url\"}")
                request.entity = reqEntity

                val response = httpClient.execute(request)
                val entity = response.entity

                if (entity != null) {
                    val jsonString = EntityUtils.toString(entity).trim { it <= ' ' }
                    return JSONArray(jsonString)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

        fun getFirstImage(list: MutableList<IMessage>): URL? {
            val pattern = Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
            var url: URL? = null
            if (url == null) {
                for (message in list) {
                    if (message.attachments.size > 0) {
                        url = URL(message.attachments[0].url)
                    } else {
                        for (split in message.content.split(" ")) {
                            val matcher = pattern.matcher(split)
                            if (matcher.find()) {
                                var urlString = split.substring(matcher.start(1), matcher.end()).replace(".webp", ".png").replace("//gyazo.com", "//i.gyazo.com")
                                if (urlString.contains("i.gyazo.com") && !urlString.endsWith(".png"))
                                    urlString += ".png"
                                url = URL(urlString)
                                break
                            }
                        }
                    }
                    if (url != null) {
                        if (url.toString().toLowerCase().endsWith(".png") || url.toString().toLowerCase().endsWith(".jpg") || url.toString().toLowerCase().endsWith(".jpeg")) {
                            break
                        }
                    }
                }
            }
            return url
        }
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