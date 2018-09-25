package me.fzzy.robofzzy4j.commands

import magick.DrawInfo
import magick.ImageInfo
import magick.MagickImage
import magick.PixelPacket
import me.fzzy.robofzzy4j.*
import org.omg.CORBA.COMM_FAILURE
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.RequestBuffer
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.net.URL
import java.util.regex.Matcher
import java.util.regex.Pattern
import javax.imageio.ImageIO
import kotlin.math.roundToInt

class Meme : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = true
    override val description = "Puts meme text onto an image"
    override val usageText: String = "-meme [imageUrl] [top text]|[bottom text]"
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
            return CommandResult.fail("Invalid syntax structure. $usageText")

        val topText = full.substring(1).replace("\n", "").split("|")[0]
        var bottomText: String? = null
        if (full.substring(1).replace("\n", "").split("|").size > 1)
            bottomText = full.substring(1).replace("\n", "").split("|")[1]

        // Find an image from the last 10 messages sent in this channel, include the one the user sent
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)

        val url: URL = ImageFuncs.getFirstImage(history) ?: return CommandResult.fail("Couldn't find image!")
        val file = ImageFuncs.downloadTempFile(url) ?: return CommandResult.fail("Couldn't download image!")

        val info = ImageInfo(file.absolutePath)
        val magickImage = MagickImage(info)
        val sizeHelper = ImageIO.read(file)

        if (topText.isNotBlank())
            annotateCenter(sizeHelper, magickImage, info, topText.toUpperCase(), false)

        if (bottomText != null)
            annotateCenter(sizeHelper, magickImage, info, bottomText.toUpperCase(), true)

        magickImage.fileName = file.absolutePath
        magickImage.writeImage(info)
        RequestBuffer.request {
            Funcs.sendFile(event.channel, file)
            file.delete()
        }
        return CommandResult.success()
    }

    fun annotateCenter(sizeHelper: BufferedImage, magickImage: MagickImage, info: ImageInfo, text: String, bottom: Boolean) {
        val aInfo = DrawInfo(info)
        aInfo.fill = PixelPacket.queryColorDatabase("white")
        aInfo.stroke = PixelPacket.queryColorDatabase("black")
        aInfo.strokeAntialias = true
        aInfo.strokeWidth = 2.0
        aInfo.opacity = 100
        aInfo.pointsize = 1.0
        aInfo.font = "Impact"
        aInfo.text = text
        val affinetransform = AffineTransform()
        val frc = FontRenderContext(affinetransform, true, true)
        var font = Font(aInfo.font, Font.PLAIN, aInfo.pointsize.toInt())
        var textWidth = font.getStringBounds(text, frc).width
        while (textWidth < sizeHelper.width * 0.75) {
            aInfo.pointsize += 1
            font = Font(aInfo.font, Font.PLAIN, aInfo.pointsize.toInt())
            textWidth = font.getStringBounds(text, frc).width
        }

        val yOffset = if (bottom) sizeHelper.height - 30 else aInfo.pointsize.roundToInt() + 20
        aInfo.geometry = "+${(sizeHelper.width / 2) - (textWidth / 2)}+$yOffset"

        magickImage.annotateImage(aInfo)
    }
}