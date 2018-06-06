package me.fzzy.eventvoter.commands

import me.fzzy.eventvoter.Command
import me.fzzy.eventvoter.downloadTempFile
import me.fzzy.eventvoter.getFirstImage
import me.fzzy.eventvoter.seam.BufferedImagePicture
import me.fzzy.eventvoter.sendMessage
import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.net.URL
import kotlin.math.roundToInt

class Deepfry : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = false
    override val description = "Deep fries an image"
    override val usageText: String = "-deepfry [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)
        val url: URL? = getFirstImage(history)
        if (url == null) {
            RequestBuffer.request { sendMessage(event.channel, "Couldn't find an image in the last 10 messages sent in this channel!") }
        } else {
            ProcessImageDeepfry(url, event).start()
        }
    }
}

private class ProcessImageDeepfry constructor(private var url: URL, private var event: MessageReceivedEvent) : Thread() {

    override fun run() {
        var processingMessage: IMessage? = null
        RequestBuffer.request { processingMessage = sendMessage(event.channel, "processing...") }

        val file = downloadTempFile(url)
        val sizeHelper = BufferedImagePicture.readFromFile(file.name)
        val width = sizeHelper.width
        val height = sizeHelper.height
        var op = IMOperation()
        val convert = ConvertCmd()
        convert.searchPath = "C:\\Program Files (x86)\\ImageMagick-6.9.9-Q16"

        op.addImage(file.name)
        op.quality(7.0)
        for (i in 0..5)
            op.contrast()
        op.noise(2.0)
        op.sharpen(10.0)
        op.resize((width / 1.5).roundToInt(), (height / 1.5).roundToInt(), '!')
        op.strip()
        op.interlace("Plane")
        op.addImage(file.name)

        convert.run(op)

        op = IMOperation()

        op.addImage(file.name)
        op.quality(7.0)
        op.noise(2.0)
        op.sharpen(10.0)
        op.resize(width, height, '!')
        op.strip()
        op.interlace("Plane")
        op.addImage(file.name)

        convert.run(op)

        RequestBuffer.request {
            processingMessage?.delete()
            try {
                event.channel.sendFile(file)
            } catch (e: MissingPermissionsException) {
            }
            file.delete()
        }
    }
}