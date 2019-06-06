package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.Guild
import me.fzzy.robofzzy4j.util.CommandCost
import me.fzzy.robofzzy4j.util.CommandResult
import me.fzzy.robofzzy4j.util.ImageHelper
import org.im4java.core.IMOperation
import org.im4java.core.ImageMagickCmd
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
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

    override fun runCommand(message: IMessage, args: List<String>): CommandResult {

        var full = ""
        for (text in args) {
            full += " $text"
        }

        val topText = full.substring(1).replace("\n", "").split("/")[0]
        var bottomText: String? = null
        if (full.substring(1).replace("\n", "").split("/").size > 1)
            bottomText = full.substring(1).replace("\n", "").split("/")[1]

        // Find an image from the last 10 messages sent in this channel, include the one the user sent
        val history = message.channel.getMessageHistory(10).toMutableList()
        history.add(0, message)

        val url = ImageHelper.getFirstImage(history)
        val file = if (url != null)
            ImageHelper.downloadTempFile(url) ?: return CommandResult.fail("i couldnt download the image ${Bot.SURPRISED_EMOJI}")
        else
            ImageHelper.createTempFile(Repost.getImageRepost(message.guild))
                    ?: return CommandResult.fail("i searched far and wide and couldnt find a picture to put your meme on ${Bot.SAD_EMOJI}")

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

        RequestBuffer.request {
            Guild.getGuild(message.guild).sendVoteAttachment(file, message.channel, message.author)
            file.delete()
        }
        return CommandResult.success()
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