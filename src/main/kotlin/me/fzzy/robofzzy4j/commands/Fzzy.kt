package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import me.fzzy.robofzzy4j.Guild.Companion.getGuild
import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import org.im4java.core.Info
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.io.File
import java.net.URL
import java.util.concurrent.Future
import javax.imageio.ImageIO
import kotlin.math.roundToInt

object Fzzy : Command {

    override val cooldownCategory = "image"
    override val cooldownMillis: Long = 60 * 1000 * 3
    override val votes: Boolean = false
    override val description = "Downsizes the last image sent in the channel using a seam carving algorithm"
    override val usageText: String = "-fzzy [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {

        // Find an image from the last 10 messages sent in this channel, include the one the user sent
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)

        val url = ImageFuncs.getFirstImage(history)
        val file = if (url != null)
            ImageFuncs.downloadTempFile(url) ?: return CommandResult.fail("i couldnt download the image")
        else
            ImageFuncs.createTempFile(Repost.getImageRepost(event.guild)) ?: return CommandResult.fail("i searched far and wide and couldnt find a picture to put your meme on :(")

        var tempFile: File? = null
        if (file.extension == "gif") {
            var op = IMOperation()

            val info = Info(file.absolutePath, false)
            var delay = info.getProperty("Delay")
            if (delay == null) {
                RequestBuffer.request { MessageScheduler.sendTempMessage(RoboFzzy.DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "i guess that picture doesnt have a framerate ¯\\_(ツ)_/¯") }
            }

            if ((delay.split("x")[1].toDouble() / delay.split("x")[0].toDouble()) < 4) {
                delay = "25x100"
            }

            val convert = ConvertCmd()

            tempFile = File("cache/${file.nameWithoutExtension}")
            tempFile.mkdirs()

            op.coalesce()
            op.addImage(file.absolutePath)
            op.addImage("cache/${tempFile.nameWithoutExtension}/temp%05d.png")

            convert.run(op)

            val futureList = arrayListOf<Future<*>>()
            for (listFile in tempFile.list()) {
                futureList.add(RoboFzzy.executor.submit {
                    resize(File("cache/${tempFile.nameWithoutExtension}/$listFile"))
                })
            }
            for (future in futureList)
                future.get()

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
                op.addImage("cache/${tempFile.nameWithoutExtension}/$listFile")
            }

            op.addImage(file.absolutePath)

            convert.run(op)
        } else
            resize(file)

        RequestBuffer.request {
            try {
                Funcs.sendFile(event.channel, file)
            } catch (e: Exception) {
            }
            file.delete()
            tempFile?.deleteRecursively()
        }
        return CommandResult.success()
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