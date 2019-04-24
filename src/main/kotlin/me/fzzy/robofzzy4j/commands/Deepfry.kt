package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.Guild
import me.fzzy.robofzzy4j.util.CommandResult
import me.fzzy.robofzzy4j.util.ImageHelper
import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import javax.imageio.ImageIO
import kotlin.math.roundToInt

object Deepfry : Command {

    override val cooldownCategory = "image"
    override val cooldownMillis: Long = 60 * 1000 * 3
    override val votes: Boolean = false
    override val description = "Deep fries an image"
    override val usageText: String = "deepfry"
    override val allowDM: Boolean = true
    override val cost: Int = 1

    override fun runCommand(message: IMessage, args: List<String>): CommandResult {
        val history = message.channel.getMessageHistory(10).toMutableList()
        history.add(0, message)

        val url = ImageHelper.getFirstImage(history)
        val file = if (url != null)
            ImageHelper.downloadTempFile(url) ?: return CommandResult.fail("i couldnt download the image")
        else
            ImageHelper.createTempFile(Repost.getImageRepost(message.guild))
                    ?: return CommandResult.fail("i searched far and wide and couldnt find a picture to put your meme on :(")

        val sizeHelper = ImageIO.read(file)
        val width = sizeHelper.width
        val height = sizeHelper.height
        var op = IMOperation()
        val convert = ConvertCmd()

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

        RequestBuffer.request {
            Guild.getGuild(message.guild).sendVoteAttachment(file, message.channel, message.author)
            file.delete()
        }
        return CommandResult.success()
    }
}