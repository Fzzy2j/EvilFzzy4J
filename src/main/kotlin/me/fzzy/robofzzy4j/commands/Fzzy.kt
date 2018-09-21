package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import org.im4java.core.Info
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.io.File
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class Fzzy : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = true
    override val description = "Downsizes the last image sent in the channel using a seam carving algorithm"
    override val usageText: String = "-fzzy [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {

        // Find an image from the last 10 messages sent in this channel, include the one the user sent
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)
        val url: URL? = ImageFuncs.getFirstImage(history)
        if (url != null) {
            Thread(Runnable {
                val file = ImageFuncs.downloadTempFile(url)
                if (file == null) {
                    RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Couldn't download image!") }
                } else {

                    var tempFile: File? = null
                    if (file.extension == "gif") {
                        var op = IMOperation()

                        val info = Info(file.name, false)
                        var delay = info.getProperty("Delay")
                        if (delay == null){
                            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Couldn't find framerate of image!") }
                        }

                        if ((delay.split("x")[1].toDouble() / delay.split("x")[0].toDouble()) < 4) {
                            delay = "25x100"
                        }

                        val convert = ConvertCmd()
                        convert.searchPath = "C:\\Program Files (x86)\\ImageMagick-6.9.9-Q16"

                        tempFile = File(file.nameWithoutExtension)
                        tempFile.mkdirs()

                        op.coalesce()
                        op.addImage(file.name)
                        op.addImage("${tempFile.nameWithoutExtension}/temp%05d.png")

                        convert.run(op)

                        for (listFile in tempFile.list()) {
                            resize(File("${tempFile.nameWithoutExtension}/$listFile"))
                        }

                        op = IMOperation()

                        op.loop(0)
                        op.dispose(2.toString())

                        //Trying to make the file smaller
                        op.deconstruct()
                        op.layers("optimize")
                        op.type("Palette")
                        op.depth(8)
                        op.fuzz(4.0, true)
                        op.dither("none")

                        op.delay(delay.split("x")[0].toInt(), delay.split("x")[1].toInt())
                        for (listFile in tempFile.list()) {
                            op.addImage("${tempFile.nameWithoutExtension}/$listFile")
                        }

                        op.addImage(file.name)

                        convert.run(op)
                    } else
                        resize(file)

                    RequestBuffer.request {
                        try {
                            Funcs.sendFile(event.channel, file)
                        }catch (e: Exception) {
                            messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Could not send generated file!")
                        }
                        file.delete()
                        tempFile?.deleteRecursively()
                    }
                }
            }).start()
        }
    }

    fun resize(file: File) {
        val sizeHelper = ImageIO.read(file)
        val op = IMOperation()
        val convert = ConvertCmd()
        convert.searchPath = "C:\\Program Files (x86)\\ImageMagick-6.9.9-Q16"

        op.addImage(file.absolutePath)
        var newWidth: Int = sizeHelper.width
        var newHeight: Int = sizeHelper.height
        val maxSize = if (file.extension == "gif") 700 else 1100
        if (sizeHelper.width > maxSize || sizeHelper.height > maxSize) {
            if (sizeHelper.width > sizeHelper.height) {
                newWidth = maxSize
                newHeight = (newWidth * sizeHelper.height) / sizeHelper.width
            } else {
                newHeight = maxSize
                newWidth = (newHeight * sizeHelper.width) / sizeHelper.height
            }
            op.resize(newWidth, newHeight, '!')
        }
        op.liquidRescale((newWidth / 1.5).roundToInt(), (newHeight / 1.5).roundToInt())
        op.addImage(file.absolutePath)

        convert.run(op)
    }

}