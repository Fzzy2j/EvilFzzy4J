package me.fzzy.evilfzzy4j.command.image

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import me.fzzy.evilfzzy4j.MessageScheduler
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandCost
import me.fzzy.evilfzzy4j.command.CommandResult
import me.fzzy.evilfzzy4j.util.ImageHelper
import org.im4java.core.IMOperation
import org.im4java.core.ImageMagickCmd
import org.im4java.core.Info
import reactor.core.publisher.Mono
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import javax.imageio.ImageIO
import kotlin.math.roundToInt

object Fzzy : Command("fzzy") {

    override val cooldownMillis: Long = 60 * 1000 * 3
    override val votes: Boolean = false
    override val description = "Downsizes the last image sent in the channel using a seam carving algorithm"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = true
    override val price: Int = 1
    override val cost: CommandCost = CommandCost.COOLDOWN

    override fun runCommand(message: CachedMessage, args: List<String>): Mono<CommandResult> {

        val url = Bot.getRecentImage(message.channel).block()
        println(url)
        val file = if (url != null)
            ImageHelper.downloadTempFile(url)
                    ?: return Mono.just(CommandResult.fail("i couldnt download the image ${Bot.surprisedEmoji()}"))
        else
            ImageHelper.createTempFile(Repost.getImageRepost(message.guild))
                    ?: return Mono.just(CommandResult.fail("i searched far and wide and couldnt find a picture to put your meme on ${Bot.sadEmoji()}"))

        var tempFile: File? = null
        if (file.extension == "gif") {
            var op = IMOperation()

            val info = Info(file.absolutePath, false)
            var delay = info.getProperty("Delay")
            if (delay == null) {
                MessageScheduler.sendTempMessage(message.channel, "i guess that picture doesnt have a framerate ¯\\_(ツ)_/¯", Bot.data.DEFAULT_TEMP_MESSAGE_DURATION).block()
            }

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
            for (listFile in tempFile.list()) {
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



        try {
            FzzyGuild.getGuild(message.guild.id).sendVoteAttachment(file, message.channel, message.author)
        } catch (e: Exception) {
        }
        file.delete()
        tempFile?.deleteRecursively()
        return Mono.just(CommandResult.success())
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