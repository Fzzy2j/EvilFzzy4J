package me.fzzy.eventvoter.commands

import magick.CompositeOperator
import magick.ImageInfo
import magick.MagickImage
import me.fzzy.eventvoter.*
import me.fzzy.eventvoter.seam.BufferedImagePicture
import org.json.JSONObject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.io.File
import java.net.URL


class Eyes : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = false
    override val description = "Adds different eyes to an image"
    override val usageText: String = "-eyes [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)
        val url: URL? = getFirstImage(history)
        if (url == null) {
            RequestBuffer.request { sendMessage(event.channel, "Couldn't find an image in the last 10 messages sent in this channel!") }
        } else {
            println(url.toString())
            ProcessImageEyes(url, event).start()
        }
    }
}

private class ProcessImageEyes constructor(private var url: URL, private var event: MessageReceivedEvent) : Thread() {

    override fun run() {
        var processingMessage: IMessage? = null
        RequestBuffer.request { processingMessage = sendMessage(event.channel, "processing...") }

        val faces = getFacialInfo("", false, true, url.toString())

        if (faces != null) {
            val file = downloadTempFile(url)
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
        }
    }
}