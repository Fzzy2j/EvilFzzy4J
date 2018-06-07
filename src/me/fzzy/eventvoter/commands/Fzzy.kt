package me.fzzy.eventvoter.commands

import magick.ImageInfo
import magick.MagickImage
import me.fzzy.eventvoter.*
import me.fzzy.eventvoter.seam.BufferedImagePicture
import me.fzzy.eventvoter.seam.SeamCarver
import me.fzzy.eventvoter.thread.ImageProcessTask
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.io.File
import java.net.URL
import javax.imageio.ImageIO

lateinit var carver: SeamCarver

class Fzzy : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = true
    override val description = "Downsizes the last image sent in the channel using a seam carving algorithm"
    override val usageText: String = "-fzzy [imageUrl]"
    override val allowDM: Boolean = true

    init {
        carver = SeamCarver()
    }

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {

        // Find an image from the last 10 messages sent in this channel, include the one the user sent
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)
        val url: URL? = ImageFuncs.getFirstImage(history)
        if (url == null) {
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Couldn't find an image in the last 10 messages sent in this channel!") }
        } else {
            imageProcessQueue.addToQueue(ProcessImageSeamCarve({
                val file = ImageFuncs.downloadTempFile(url)
                if (file == null) {
                    RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Couldn't download image!") }
                } else {
                    val info = ImageInfo(file.name)
                    var magickImage = MagickImage(info)
                    var sizeHelper = ImageIO.read(file)
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
                    val image = BufferedImagePicture.readFromFile(file.name)
                    val scale = carver.resize(image, image.width / 3, image.height / 3)

                    ImageIO.write(scale.image, "jpg", file)
                    file
                }
            }, event.channel))
        }
    }
}

private class ProcessImageSeamCarve(override val code: () -> Any?, private var channel: IChannel) : ImageProcessTask {

    var processingMessage: IMessage? = null

    override fun queueUpdated(position: Int) {
        val msg = if (position == 0) "processing..." else "position in queue: $position"
        RequestBuffer.request {
            if (processingMessage == null)
                processingMessage = Funcs.sendMessage(channel, msg)
            else
                processingMessage?.edit(msg)
        }
    }

    override fun finished(obj: Any?) {
        if (obj == null) {
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, channel, "No faces detected in image.") }
        } else {
            if (obj is File) {
                RequestBuffer.request {
                    processingMessage?.delete()
                    messageScheduler.sendTempFile(60 * 1000, channel, obj)
                    obj.delete()
                }
            }
        }
    }
}