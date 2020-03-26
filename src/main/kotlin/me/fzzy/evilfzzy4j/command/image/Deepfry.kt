package me.fzzy.evilfzzy4j.command.image

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.im4java.core.IMOperation
import org.im4java.core.ImageMagickCmd
import javax.imageio.ImageIO
import kotlin.math.roundToInt

object Deepfry : Command("deepfry") {

    override val cooldownMillis: Long = 60 * 1000
    override val description = "Deep fries an image"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {
        val file = Bot.getRecentImage(event.channel, latestMessageId)
                ?: return CommandResult.fail("i couldnt get an image file ${Bot.sadEmote.asMention}")

        val sizeHelper = ImageIO.read(file)
        val width = sizeHelper.width
        val height = sizeHelper.height
        var op = IMOperation()
        val convert = ImageMagickCmd("convert")

        op.addImage(file.absolutePath)
        op.quality(6.0)
        op.contrast()
        op.addImage("noise.png")
        op.compose("dissolve")
        op.define("compose:args=\"20\"")
        op.composite()
        op.enhance()
        op.resize((width / 1.6).roundToInt(), (height / 3.0).roundToInt(), '!')
        op.addImage(file.absolutePath)

        convert.run(op)

        op = IMOperation()

        op.addImage(file.absolutePath)
        op.quality(5.0)
        op.sharpen(10.0)
        op.resize(width, height, '!')
        op.addImage(file.absolutePath)

        convert.run(op)

        event.textChannel.sendFile(file).queue()

        return CommandResult.success()
    }
}