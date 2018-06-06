package me.fzzy.eventvoter.commands

import magick.ImageInfo
import magick.MagickImage
import me.fzzy.eventvoter.Command
import me.fzzy.eventvoter.imageQueues
import me.fzzy.eventvoter.seam.BufferedImagePicture
import me.fzzy.eventvoter.seam.SeamCarver
import me.fzzy.eventvoter.sendMessage
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
import javax.imageio.ImageIO

lateinit var carver: SeamCarver

class Fzzy : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = false
    override val description = "Downsizes the last image sent in the channel using a seam carving algorithm"
    override val usageText: String = "-fzzy [imageUrl]"
    override val allowDM: Boolean = true

    init {
        carver = SeamCarver()
    }

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
            ProcessImageSeamCarve(url, event).start()
        }
    }
}

private class ProcessImageSeamCarve constructor(private var url: URL, private var event: MessageReceivedEvent) : Thread() {

    override fun run() {
        val fileName = "${imageQueues++}.jpg"
        var processingMessage: IMessage? = null
        RequestBuffer.request { processingMessage = sendMessage(event.channel, "processing...") }
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

        ImageIO.write(scale.image, "jpg", file)

        RequestBuffer.request {
            processingMessage?.delete()
            try {
                event.channel.sendFile(file)
            } catch (e: MissingPermissionsException) {
            }
            file.delete()
        }
        if (imageQueues > 1000000)
            imageQueues = 0
    }
}