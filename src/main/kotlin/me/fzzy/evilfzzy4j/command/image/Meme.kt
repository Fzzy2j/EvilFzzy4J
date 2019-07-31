package me.fzzy.evilfzzy4j.command.image

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandCost
import me.fzzy.evilfzzy4j.command.CommandResult
import me.fzzy.evilfzzy4j.util.ImageHelper
import org.im4java.core.IMOperation
import org.im4java.core.ImageMagickCmd
import reactor.core.publisher.Mono
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.io.File
import javax.imageio.ImageIO

object Meme : Command("meme") {

    override val cooldownMillis: Long = 60 * 1000 * 3
    override val votes: Boolean = false
    override val description = "Puts meme text onto an image"
    override val args: ArrayList<String> = arrayListOf("text")
    override val allowDM: Boolean = true
    override val price: Int = 1
    override val cost: CommandCost = CommandCost.COOLDOWN

    override fun runCommand(message: CachedMessage, args: List<String>): Mono<CommandResult> {

        var full = ""
        for (text in args) {
            full += " $text"
        }

        val topText = full.substring(1).replace("\n", "").split("/")[0]
        var bottomText: String? = null
        if (full.substring(1).replace("\n", "").split("/").size > 1)
            bottomText = full.substring(1).replace("\n", "").split("/")[1]

        val url = Bot.getRecentImage(message.channel).block()
        val file = if (url != null)
            ImageHelper.downloadTempFile(url) ?: return Mono.just(CommandResult.fail("i couldnt download the image ${Bot.surprisedEmoji()}"))
        else
            ImageHelper.createTempFile(Repost.getImageRepost(message.guild))
                    ?: return Mono.just(CommandResult.fail("i searched far and wide and couldnt find a picture to put your meme on ${Bot.sadEmoji()}"))

        val convert = ImageMagickCmd("convert")
        val operation = IMOperation()

        operation.addImage(file.absolutePath)

        operation.fill("white")
        operation.font("Impact")
        operation.stroke("black")
        operation.strokewidth(2)

        if (topText.isNotBlank())
            annotateCenter(file, operation, topText.toUpperCase(), false)

        if (bottomText != null)
            annotateCenter(file, operation, bottomText.toUpperCase(), true)

        operation.addImage(file.absolutePath)

        convert.run(operation)


        FzzyGuild.getGuild(message.guild.id).sendVoteAttachment(file, message.channel, message.author)
        file.delete()
        return Mono.just(CommandResult.success())
    }

    fun annotateCenter(imgFile: File, operation: IMOperation, text: String, bottom: Boolean) {
        val img = ImageIO.read(imgFile)
        val affine = AffineTransform()
        val frc = FontRenderContext(affine, true, true)
        var pointSize = 1
        var font = Font("Impact", Font.PLAIN, pointSize)
        var textWidth = font.getStringBounds(text, frc).width
        while (textWidth < img.width * 0.75) {
            pointSize += 1
            font = Font("Impact", Font.PLAIN, pointSize)
            textWidth = font.getStringBounds(text, frc).width
        }

        if (bottom)
            operation.gravity("south")
        else
            operation.gravity("north")

        operation.pointsize(pointSize)
        operation.draw("\"text 0,20 '$text'\"")
    }
}