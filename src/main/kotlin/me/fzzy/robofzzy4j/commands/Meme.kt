package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import org.omg.CORBA.COMM_FAILURE
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.RequestBuffer
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.net.URL
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.imageio.ImageIO
import kotlin.math.roundToInt

object Meme : Command {

    override val cooldownCategory = "image"
    override val cooldownMillis: Long = 60 * 1000 * 3
    override val votes: Boolean = false
    override val description = "Puts meme text onto an image"
    override val usageText: String = "-meme <top text>|<bottom text>"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {

        var full = ""
        for (text in args) {
            val pattern = Pattern.compile("((http:\\/\\/|https:\\/\\/)?(www.)?(([a-zA-Z0-9-]){2,}\\.){1,4}([a-zA-Z]){2,6}(\\/([a-zA-Z-_\\/\\.0-9#:?=&;,]*)?)?)")
            val m: Matcher = pattern.matcher(text)
            if (!m.find()) {
                full += " $text"
            }
        }

        if (full.isBlank())
            return CommandResult.fail("i dont know what you mean $usageText")

        val topText = full.substring(1).replace("\n", "").split("|")[0]
        var bottomText: String? = null
        if (full.substring(1).replace("\n", "").split("|").size > 1)
            bottomText = full.substring(1).replace("\n", "").split("|")[1]

        // Find an image from the last 10 messages sent in this channel, include the one the user sent
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)

        val url = ImageFuncs.getFirstImage(history)
        val file = if (url != null)
            ImageFuncs.downloadTempFile(url) ?: return CommandResult.fail("i couldnt download the image")
        else
            ImageFuncs.createTempFile(Repost.getImageRepost(event.guild)) ?: return CommandResult.fail("i searched far and wide and couldnt find a picture to put your meme on :(")

        val convert = ConvertCmd()
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
            Funcs.sendFile(event.channel, file)
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
        operation.draw("\"text 0,20 $text\"")
    }
}