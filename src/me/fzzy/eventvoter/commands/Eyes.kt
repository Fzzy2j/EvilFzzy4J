package me.fzzy.eventvoter.commands

import magick.CompositeOperator
import magick.ImageInfo
import magick.MagickImage
import me.fzzy.eventvoter.Command
import me.fzzy.eventvoter.TempMessage
import me.fzzy.eventvoter.imageQueues
import me.fzzy.eventvoter.seam.BufferedImagePicture
import me.fzzy.eventvoter.sendMessage
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.utils.URIBuilder
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import org.json.JSONArray
import org.json.JSONObject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.regex.Pattern


class Eyes : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = false
    override val description = "Adds different eyes to an image"
    override val usageText: String = "-eyes [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        val pattern = Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
        var url: URL? = null
        if (args.size == 1) {
            val commandMatches = pattern.matcher(args[0])
            if (commandMatches.find()) {
                url = URL(args[0].substring(commandMatches.start(1), commandMatches.end()))
            }
        }
        if (url == null) {
            for (message in event.channel.getMessageHistory(10)) {
                if (message.attachments.size > 0) {
                    url = URL(message.attachments[0].url)
                } else {
                    val matcher = pattern.matcher(message.content)
                    if (matcher.find()) {
                        url = URL(message.content.substring(matcher.start(1), matcher.end()).replace(".webp", ".png"))
                    }
                }
                if (url != null) {
                    if (url.toString().toLowerCase().endsWith(".png") || url.toString().toLowerCase().endsWith(".jpg") || url.toString().toLowerCase().endsWith(".jpeg")) {
                        break
                    }
                }
            }
        }
        if (url == null) {
            RequestBuffer.request { sendMessage(event.channel, "Couldn't find an image in the last 10 messages sent in this channel!") }
        } else {
            ProcessImageEyes(url, event).start()
        }
    }
}

private class ProcessImageEyes constructor(private var url: URL, private var event: MessageReceivedEvent) : Thread() {

    override fun run() {
        val apiurl = "https://westcentralus.api.cognitive.microsoft.com/face/v1.0/detect"
        val attr = "emotion,headPose,gender"
        val httpClient = HttpClientBuilder.create().build()
        try {
            val builder = URIBuilder(apiurl)
            var processingMessage: IMessage? = null
            RequestBuffer.request { processingMessage = sendMessage(event.channel, "processing...") }

            builder.setParameter("returnFaceId", "false")
            builder.setParameter("returnFaceLandmarks", "true")
            builder.setParameter("returnFaceAttributes", attr)

            val uri = builder.build()
            val request = HttpPost(uri)

            request.setHeader("Content-Type", "application/json")
            request.setHeader("Ocp-Apim-Subscription-Key", "6ecaebf03a5a4c78b811c2f13d0a2098")

            val reqEntity = StringEntity("{\"url\":\"$url\"}")
            request.entity = reqEntity

            val response = httpClient.execute(request)
            val entity = response.entity

            if (entity != null) {
                val jsonString = EntityUtils.toString(entity).trim { it <= ' ' }
                val faces = JSONArray(jsonString)

                val fileName = "${imageQueues++}.jpg"
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

                val file = File(fileName)
                val info = ImageInfo(file.name)
                val magickImage = MagickImage(info)

                val eyeFile = File("redlight.png")
                val eyeInfo = ImageInfo(eyeFile.name)
                val eyeMagickImage = MagickImage(eyeInfo)
                val sizeHelper = BufferedImagePicture.readFromFile(eyeMagickImage.fileName)
                if (faces.length() > 0) {
                    for (face in faces) {
                        if (face is JSONObject) {
                            val left = face.getJSONObject("faceLandmarks").getJSONObject("pupilLeft")
                            val right = faces.getJSONObject(0).getJSONObject("faceLandmarks").getJSONObject("pupilRight")

                            magickImage.compositeImage(CompositeOperator.OverCompositeOp, eyeMagickImage, left.getInt("x") - sizeHelper.width / 2, left.getInt("y") - sizeHelper.height / 2)
                            magickImage.compositeImage(CompositeOperator.OverCompositeOp, eyeMagickImage, right.getInt("x") - sizeHelper.width / 2, right.getInt("y") - sizeHelper.height / 2)
                        }
                    }

                    magickImage.fileName = file.absolutePath
                    magickImage.writeImage(info)

                    RequestBuffer.request {
                        processingMessage?.delete()
                        try {
                            event.channel.sendFile(file)
                        } catch (e: MissingPermissionsException) {
                        }
                        file.delete()
                    }
                } else {
                    RequestBuffer.request {
                        val msg = sendMessage(event.channel, "No faces detected in image.")
                        if (msg != null)
                            TempMessage(7 * 1000, msg).start()
                        processingMessage?.delete()
                        file.delete()
                    }
                }
                if (imageQueues > 1000000)
                    imageQueues = 0
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}