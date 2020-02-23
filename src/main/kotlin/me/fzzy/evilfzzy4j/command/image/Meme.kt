package me.fzzy.evilfzzy4j.command.image

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.im4java.core.IMOperation
import org.im4java.core.ImageMagickCmd
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.io.File
import javax.imageio.ImageIO

object Meme : Command("meme") {

    override val cooldownMillis: Long = 60 * 1000 * 3
    override val description = "Puts meme text onto an image"
    override val args: ArrayList<String> = arrayListOf("text")
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {

        val full = args.joinToString(" ").replace("'", "").replace("\n", "").split("|")

        val topText = full[0]
        var bottomText: String? = null
        if (full.size > 1) bottomText = full[1]

        val file = Bot.getRecentImage(event.channel, latestMessageId)
                ?: return CommandResult.fail("i couldnt get an image file ${Bot.sadEmoji.asMention}")

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


        FzzyGuild.getGuild(event.guild.id).sendVoteAttachment(file, event.channel as TextChannel, event.author)
        file.delete()
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