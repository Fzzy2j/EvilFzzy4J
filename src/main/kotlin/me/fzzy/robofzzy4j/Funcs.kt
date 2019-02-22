package me.fzzy.robofzzy4j

import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.json.JSONArray
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.MissingPermissionsException
import java.io.*
import java.net.URL
import java.nio.file.Files
import java.util.*
import java.util.regex.Pattern
import javax.net.ssl.HttpsURLConnection
import java.util.LinkedHashMap
import java.util.ArrayList
import java.util.HashMap


object Funcs {
    fun sendMessage(channel: IChannel, text: String): IMessage? {
        if (text.isEmpty())
            return null
        return try {
            channel.sendMessage(text)
        } catch (e: MissingPermissionsException) {
            null
        }
    }

    fun sendEmbed(channel: IChannel, embed: EmbedObject): IMessage? {
        return try {
            channel.sendMessage(embed)
        } catch (e: MissingPermissionsException) {
            null
        }
    }

    fun sendFile(channel: IChannel, file: File, votes: Boolean = true): IMessage? {
        return try {
            val message = channel.sendFile(file)
            if (votes)
                Guild.getGuild(channel.guild.longID).allowVotes(message)
            message
        } catch (e: MissingPermissionsException) {
            null
        }
    }

    fun mentionsByName(msg: IMessage): Boolean {
        val check = msg.content.toLowerCase()
        val checkAgainst = "${RoboFzzy.cli.ourUser.name} ${RoboFzzy.cli.ourUser.getDisplayName(msg.guild)}"
        for (realCheck in check.split(" ")) {
            if (check.contains("thank") && (checkAgainst.toLowerCase().replace(" ", "").contains(realCheck) || msg.mentions.contains(RoboFzzy.cli.ourUser)))
                return true
        }
        return false
    }

    fun getTextToSpeech(text: String): ByteArray? {
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
                "<voice xml:lang='en-US' name='Microsoft Server Speech Text to Speech Voice ${voices[RoboFzzy.random.nextInt(voices.size)]}'>" +
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
            urlConnection.setRequestProperty("Authorization", "Bearer ${RoboFzzy.auth.GetAccessToken()}")
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

    fun sortHashMapByValues(passedMap: HashMap<Long, Int>): LinkedHashMap<Long, Int> {
        val mapKeys = ArrayList(passedMap.keys)
        val mapValues = ArrayList(passedMap.values)
        mapValues.sort()
        mapKeys.sort()

        val sortedMap = LinkedHashMap<Long, Int>()

        val valueIt = mapValues.iterator()
        while (valueIt.hasNext()) {
            val `val` = valueIt.next()
            val keyIt = mapKeys.iterator()

            while (keyIt.hasNext()) {
                val key = keyIt.next()
                val comp1 = passedMap[key]

                if (comp1 == `val`) {
                    keyIt.remove()
                    sortedMap[key] = `val`
                    break
                }
            }
        }
        return sortedMap
    }
}

object ImageFuncs {

    fun getMinecraftAchievement(text: String): URL {
        val url = "https://mcgen.herokuapp.com/a.php?i=${Random().nextInt(20) + 1}&h=Achievement+get!&t=$text"
        return URL(url)
    }

    fun downloadTempFile(url: URL): File? {
        val fixedUrl = URL(url.toString().replace(".gifv", ".gif"))
        var suffix = "jpg"
        if (fixedUrl.toString().endsWith("webp") || fixedUrl.toString().endsWith("png"))
            suffix = "png"
        if (fixedUrl.toString().endsWith("gif"))
            suffix = "gif"

        val fileName = "cache/${System.currentTimeMillis()}.$suffix"
        try {
            val openConnection = fixedUrl.openConnection()
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
        return File(fileName)
    }

    fun createTempFile(file: File?): File? {
        if (file == null) return null
        val new = File("cache/${System.currentTimeMillis()}.${file.extension}")
        Files.copy(file.toPath(), new.toPath())
        return new
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
            request.setHeader("Ocp-Apim-Subscription-Key", RoboFzzy.faceApiToken)

            val reqEntity = StringEntity("{\"url\":\"$url\"}")
            request.entity = reqEntity

            val response = httpClient.execute(request)
            val entity = response.entity

            if (entity != null) {
                val jsonString = EntityUtils.toString(entity).trim { it <= ' ' }
                try {
                    return JSONArray(jsonString)
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getFirstImage(list: MutableList<IMessage>): URL? {
        val pattern = Pattern.compile("(?:^|[\\W])((ht|f)tp(s?):\\/\\/)"
                + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
                + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)")
        var url: URL? = null
        for (message in list) {
            if (message.attachments.size > 0) {
                url = URL(message.attachments[0].url)
            } else {
                for (split in message.content.split(" ")) {
                    val matcher = pattern.matcher(split)
                    if (matcher.find()) {
                        var urlString = split.toLowerCase().substring(matcher.start(1), matcher.end()).replace(".webp", ".png").replace("//gyazo.com", "//i.gyazo.com")
                        if (urlString.contains("i.gyazo.com") && !urlString.endsWith(".png")) {
                            urlString += ".png"
                        }
                        if (urlString.contains("i.gyazo.com") && !urlString.endsWith(".jpg")) {
                            urlString += ".jpq"
                        }
                        if (urlString.endsWith("png") || urlString.endsWith("jpg") || urlString.endsWith("jpeg") || urlString.endsWith("gif")) {
                            url = URL(urlString)
                            break
                        }
                    }
                }
            }
            if (url != null) {
                if (url.toString().toLowerCase().endsWith(".png")
                        || url.toString().toLowerCase().endsWith(".jpg")
                        || url.toString().toLowerCase().endsWith(".jpeg")
                        || url.toString().toLowerCase().endsWith(".gif")) {
                    break
                }
            }
        }
        return url
    }
}