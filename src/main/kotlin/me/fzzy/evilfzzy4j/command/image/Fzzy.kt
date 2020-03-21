package me.fzzy.evilfzzy4j.command.image

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.im4java.core.IMOperation
import org.im4java.core.ImageMagickCmd
import org.im4java.core.Info
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.imageio.ImageIO
import kotlin.math.roundToInt

object Fzzy : Command("fzzy") {

    override val cooldownMillis: Long = 60 * 1000 * 3
    override val description = "Downsizes the last image sent in the channel using a seam carving algorithm"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {
        val file = Bot.getRecentImage(event.channel, latestMessageId)
                ?: return CommandResult.fail("i couldnt get an image file ${Bot.sadEmote.asMention}")

        var tempFile: File? = null
        if (file.extension == "gif") {
            var op = IMOperation()

            val info = Info(file.absolutePath, false)
            var delay = info.getProperty("Delay")
                    ?: return CommandResult.fail("i cant find a framerate for this file ${Bot.sadEmote.asMention}")

            if ((delay.split("x")[1].toDouble() / delay.split("x")[0].toDouble()) < 4) {
                delay = "25x100"
            }

            val convert = ImageMagickCmd("convert")

            tempFile = File("cache/${file.nameWithoutExtension}")
            tempFile.mkdirs()

            op.coalesce()
            op.addImage(file.absolutePath)
            op.addImage("cache/${tempFile.nameWithoutExtension}/temp%05d.png")

            convert.run(op)

            val executor = Executors.newFixedThreadPool(4)
            val futureList = arrayListOf<Future<*>>()
            for (listFile in tempFile.list() ?: return CommandResult.fail("an unknown error occured")) {
                futureList.add(executor.submit {
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
                op.addImage("cache${File.separator}${tempFile.nameWithoutExtension}${File.separator}$listFile")
            }

            op.addImage(file.absolutePath)

            convert.run(op)
        } else
            resize(file)


        event.textChannel.sendFile(file).queue()
        file.delete()
        tempFile?.deleteRecursively()
        return CommandResult.success()
    }

    fun resize(file: File) {
        val sizeHelper = ImageIO.read(file)
        val op = IMOperation()
        val convert = ImageMagickCmd("convert")

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