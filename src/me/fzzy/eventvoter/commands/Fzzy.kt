package me.fzzy.eventvoter.commands

import magick.ImageInfo
import magick.MagickImage
import me.fzzy.eventvoter.Command
import me.fzzy.eventvoter.carver
import me.fzzy.eventvoter.seam.BufferedImagePicture
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.regex.Pattern
import javax.imageio.ImageIO

class Fzzy : Command {

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        val pattern = Pattern.compile("((http:\\/\\/|https:\\/\\/)?(www.)?(([a-zA-Z0-9-]){2,}\\.){1,4}([a-zA-Z]){2,6}(\\/([a-zA-Z-_\\/\\.0-9#:?=&;,]*)?)?)")
        var url: URL? = null
        for (message in event.channel.getMessageHistory(10)) {
            if (message.attachments.size > 0) {
                url = URL(message.attachments[0].url)
            } else {
                val matcher = pattern.matcher(message.content)
                if (matcher.find()) {
                    url = URL(message.content.substring(matcher.start(1), matcher.end()))
                }
            }
            if (url != null) {
                if (url.toString().toLowerCase().endsWith(".png") || url.toString().toLowerCase().endsWith(".jpg")) {
                    break
                }
            }
        }
        if (url == null) {
            RequestBuffer.request { event.channel.sendMessage("Couldn't find an image in the last 10 messages sent in this channel!") }
        } else {
            ProcessImage(url, event).start()
        }
    }
}

class ProcessImage constructor(private var url: URL, private var event: MessageReceivedEvent) : Thread() {

    override fun run() {
        var processingMessage: IMessage? = null
        RequestBuffer.request { processingMessage = event.channel.sendMessage("processing...") }
        val openConnection = url.openConnection()
        openConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
        openConnection.connect()

        val inputStream = BufferedInputStream(openConnection.getInputStream())
        val outputStream = BufferedOutputStream(FileOutputStream("temp.jpg"))

        for (out in inputStream.iterator()) {
            outputStream.write(out.toInt())
        }
        inputStream.close()
        outputStream.close()

        val file = File("temp.jpg")
        val info = ImageInfo(file.name)
        var magickImage = MagickImage(info)
        var sizeHelper = BufferedImagePicture.readFromFile(file.name)
        if (sizeHelper.width > 800 || sizeHelper.height > 800) {
            val newWidth: Int
            val newHeight: Int
            if (sizeHelper.width > sizeHelper.height) {
                newWidth = 800
                newHeight = (newWidth * sizeHelper.height) / sizeHelper.width
            } else {
                newHeight = 800
                newWidth = (newHeight * sizeHelper.width) / sizeHelper.height
            }
            magickImage = magickImage.scaleImage(newWidth, newHeight)
            magickImage.fileName = file.absolutePath
            magickImage.writeImage(info)
        }
        var image = BufferedImagePicture.readFromFile(file.name)
        val scale = carver.resize(image, image.width / 3, image.height / 3)

        //blend
        //image = Rescaler.rescaleImageWidth(image, image.width / 3)
        //image = Rescaler.rescaleImageHeight(image, image.width / 3)
        ImageIO.write(scale.image, "jpg", file)

        RequestBuffer.request {
            processingMessage?.delete()
            event.channel.sendFile(file)
            file.delete()
        }
    }
}