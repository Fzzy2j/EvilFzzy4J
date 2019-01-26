package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import org.apache.commons.io.FileUtils
import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import org.im4java.core.Info
import org.im4java.core.MogrifyCmd
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.io.File
import java.net.URL
import java.util.concurrent.Future
import javax.imageio.ImageIO
import kotlin.math.roundToInt

object Explode : Command {

    override val cooldownCategory = "image"
    override val cooldownMillis: Long = 60 * 1000 * 3
    override val votes: Boolean = false
    override val description = "Scales an image repeatedly, turning it into a gif"
    override val usageText: String = "-explode [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {

        // Find an image from the last 10 messages sent in this channel, include the one the user sent
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)

        val url: URL = ImageFuncs.getFirstImage(history)
                ?: return CommandResult.fail("theres no image in the last 10 messages?")

        var file = ImageFuncs.downloadTempFile(url) ?: return CommandResult.fail("i couldnt download the image")
        var tempFile: File? = null
        val finalSize = 0.3
        val convert = ConvertCmd()

        if (file.extension == "gif") {
            var op = IMOperation()

            val info = Info(file.absolutePath, false)
            var delay = info.getProperty("Delay")
            if (delay == null) {
                RequestBuffer.request { MessageScheduler.sendTempMessage(RoboFzzy.DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "this image has no framerate to it, i cant work with it") }
            }

            if ((delay.split("x")[1].toDouble() / delay.split("x")[0].toDouble()) < 4) {
                delay = "25x100"
            }

            tempFile = File("cache/${file.nameWithoutExtension}")
            tempFile.mkdirs()

            op.coalesce()
            op.addImage(file.absolutePath)
            op.addImage("${tempFile.absolutePath}/temp%05d.png")

            convert.run(op)

            val fileList = tempFile.list()
            val futureList = arrayListOf<Future<*>>()
            val frames = hashMapOf<Int, String>()
            for (i in 0 until fileList.size) {
                futureList.add(RoboFzzy.executor.submit {
                    val listFile = fileList[i]

                    // https://www.desmos.com/calculator/gztrr4yh2w
                    val initialSize = 1.0
                    val sizeAmt = -(i / (fileList.size.toDouble() / (initialSize - finalSize))) + initialSize
                    resize(File("${tempFile.absolutePath}/$listFile"), sizeAmt)
                })
            }
            var progress = 0
            futureList.forEach { future ->
                future.get()
                progress++
            }
            for (i in 0 until fileList.size)
                op.addImage(frames[i])

            op = IMOperation()

            //Trying to make the file smaller
            op.deconstruct()
            op.layers("optimize")
            op.type("Palette")
            op.depth(8)
            op.fuzz(6.0, true)
            op.dither("none")

            op.loop(0)
            op.dispose(2.toString())
            op.delay(delay.split("x")[0].toInt(), delay.split("x")[1].toInt())
            for (listFile in tempFile.list()) {
                op.addImage("${tempFile.absolutePath}/$listFile")
            }

            op.addImage(file.absolutePath)


            convert.run(op)
        } else {

            // Construct a gif out of a single image
            val op = IMOperation()

            //Trying to make the file smaller
            op.deconstruct()
            op.layers("optimize")
            op.type("Palette")
            op.depth(8)
            op.fuzz(6.0, true)
            op.dither("none")

            op.loop(0)
            op.dispose(2.toString())
            op.delay(10, 100)
            val tempPath = File("cache/${file.nameWithoutExtension}")
            tempPath.mkdirs()
            val frameCount = 20
            val futureList = arrayListOf<Future<*>>()
            val frames = hashMapOf<Int, String>()
            for (i in 0..frameCount) {
                futureList.add(RoboFzzy.executor.submit {
                    val child = "temp$i.${file.extension}"
                    val warpFile = File(tempPath, child)
                    FileUtils.copyFile(file, warpFile)

                    // https://www.desmos.com/calculator/gztrr4yh2w
                    val initialSize = 1.0
                    val sizeAmt = -(i / (frameCount.toDouble() / (initialSize - finalSize))) + initialSize
                    resize(warpFile, sizeAmt)
                    frames[i] = warpFile.absolutePath
                })
            }
            var progress = 0
            futureList.forEach { future ->
                future.get()
                progress++
            }
            for (i in 0..frameCount)
                op.addImage(frames[i])
            val result = File("cache/${file.nameWithoutExtension}.gif")
            op.addImage(result.absolutePath)

            file.delete()
            convert.run(op)
            tempPath.deleteRecursively()

            file = result
        }

        RequestBuffer.request {
            try {
                Funcs.sendFile(event.channel, file)
            } catch (e: Exception) {
                event.channel.sendMessage("i couldnt send the file, sorry")
            }
            file.delete()
            tempFile?.deleteRecursively()
        }
        return CommandResult.success()
    }

    fun resize(file: File, size: Double) {
        val sizeHelper = ImageIO.read(file)
        val op = IMOperation()
        val convert = ConvertCmd()
        convert.searchPath = "C:\\Program Files (x86)\\ImageMagick-6.9.9-Q16"

        op.addImage(file.absolutePath)
        var newWidth: Int = sizeHelper.width
        var newHeight: Int = sizeHelper.height
        val maxSize = 700
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
        op.liquidRescale((newWidth * size).roundToInt(), (newHeight * size).roundToInt())
        op.resize(newWidth, newHeight, '!')
        op.addImage(file.absolutePath)

        convert.run(op)
    }

}