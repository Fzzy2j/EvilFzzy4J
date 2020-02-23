package me.fzzy.evilfzzy4j.command.image

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.im4java.core.IMOperation
import org.im4java.core.ImageMagickCmd
import org.im4java.core.Info
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.imageio.ImageIO
import kotlin.math.roundToInt

object Explode : Command("explode") {

    override val cooldownMillis: Long = 60 * 1000 * 5
    override val description = "Scales an image repeatedly, turning it into a gif"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {
        var file = Bot.getRecentImage(event.channel, latestMessageId)?: return CommandResult.fail("i couldnt get an image file ${Bot.sadEmoji.asMention}")
        val finalSize = 0.3
        val convert = ImageMagickCmd("convert")
        val executor = Executors.newFixedThreadPool(4)

        if (file.extension == "gif") {
            var op = IMOperation()

            val info = Info(file.absolutePath, false)
            var delay = info.getProperty("Delay")?: return CommandResult.fail("i cant find a framerate for this file ${Bot.sadEmoji.asMention}")

            if ((delay.split("x")[1].toDouble() / delay.split("x")[0].toDouble()) < 4) {
                delay = "25x100"
            }

            val tempFile = File("cache/${file.nameWithoutExtension}")
            tempFile.mkdirs()

            op.coalesce()
            op.addImage(file.absolutePath)
            op.addImage("${tempFile.absolutePath}/temp%05d.png")

            convert.run(op)

            val fileList = tempFile.list()?: return CommandResult.fail("an unknown error occured")
            val futureList = arrayListOf<Future<*>>()
            for (i in fileList.indices) {
                futureList.add(executor.submit {
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
            for (element in fileList)
                op.addImage(element)

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
            for (listFile in tempFile.list()!!) {
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
                futureList.add(executor.submit {
                    val child = "temp$i.${file.extension}"
                    val warpFile = File(tempPath, child)
                    Files.copy(file.toPath(), warpFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

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


        FzzyGuild.getGuild(event.guild.id).sendVoteAttachment(file, event.channel as TextChannel, event.author)
        return CommandResult.success()
    }

    fun resize(file: File, size: Double) {
        val sizeHelper = ImageIO.read(file)
        val op = IMOperation()
        val convert = ImageMagickCmd("convert")

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