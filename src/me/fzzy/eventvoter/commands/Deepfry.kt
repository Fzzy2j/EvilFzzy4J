package me.fzzy.eventvoter.commands

import me.fzzy.eventvoter.*
import me.fzzy.eventvoter.seam.BufferedImagePicture
import me.fzzy.eventvoter.thread.ImageProcessTask
import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.io.File
import java.net.URL
import kotlin.math.roundToInt

class Deepfry : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = true
    override val description = "Deep fries an image"
    override val usageText: String = "-deepfry [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)
        val url: URL? = ImageFuncs.getFirstImage(history)
        if (url == null) {
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Couldn't find an image in the last 10 messages sent in this channel!") }
        } else {
            imageProcessQueue.addToQueue(ProcessImageDeepfry({
                val file = ImageFuncs.downloadTempFile(url)

                if (file == null) {
                    RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Couldn't download image!") }
                } else {
                    val sizeHelper = BufferedImagePicture.readFromFile(file.name)
                    val width = sizeHelper.width
                    val height = sizeHelper.height
                    var op = IMOperation()
                    val convert = ConvertCmd()
                    convert.searchPath = "C:\\Program Files (x86)\\ImageMagick-6.9.9-Q16"

                    op.addImage(file.name)
                    op.quality(8.0)
                    for (i in 0..3)
                        op.contrast()
                    op.noise(2.0)
                    op.sharpen(10.0)
                    op.resize((width / 1.5).roundToInt(), (height / 1.5).roundToInt(), '!')
                    op.addImage(file.name)

                    convert.run(op)

                    op = IMOperation()

                    op.addImage(file.name)
                    op.quality(7.0)
                    op.noise(2.0)
                    op.sharpen(10.0)
                    op.resize(width, height, '!')
                    op.addImage(file.name)

                    convert.run(op)

                    file
                }
            }, event.channel))
        }
    }
}

private class ProcessImageDeepfry(override val code: () -> Any?, private var channel: IChannel) : ImageProcessTask {

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
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, channel, "Couldn't download image!") }
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