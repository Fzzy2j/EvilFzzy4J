package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import org.im4java.core.IMOperation
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.net.URL
import javax.imageio.ImageIO
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
            Thread(Runnable {
                val file = ImageFuncs.downloadTempFile(url)

                if (file == null) {
                    RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Couldn't download image!") }
                } else {
                    val sizeHelper = ImageIO.read(file)
                    val width = sizeHelper.width
                    val height = sizeHelper.height
                    var op = IMOperation()

                    op.addImage(file.name)
                    op.quality(6.0)
                    op.contrast()
                    op.addImage("noise.png")
                    op.compose("dissolve")
                    op.define("compose:args=\"20\"")
                    op.composite()
                    op.enhance()
                    op.resize((width / 1.6).roundToInt(), (height / 3.0).roundToInt(), '!')
                    op.addImage(file.name)

                    convert.run(op)

                    op = IMOperation()

                    op.addImage(file.name)
                    op.quality(5.0)
                    op.sharpen(10.0)
                    op.resize(width, height, '!')
                    op.addImage(file.name)

                    convert.run(op)

                    RequestBuffer.request {
                        Funcs.sendFile(event.channel, file)
                        file.delete()
                    }
                }
            }).start()
        }
    }
}