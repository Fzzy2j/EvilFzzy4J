package me.fzzy.robofzzy4j.command.image

import discord4j.core.`object`.entity.Message
import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.FzzyGuild
import me.fzzy.robofzzy4j.command.Command
import me.fzzy.robofzzy4j.command.CommandCost
import me.fzzy.robofzzy4j.command.CommandResult
import me.fzzy.robofzzy4j.util.ImageHelper
import org.im4java.core.IMOperation
import org.im4java.core.ImageMagickCmd
import reactor.core.publisher.Mono
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToInt

object Deepfry : Command("deepfry") {

    override val cooldownMillis: Long = 60 * 1000 * 3
    override val votes: Boolean = false
    override val description = "Deep fries an image"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = true
    override val price: Int = 1
    override val cost: CommandCost = CommandCost.COOLDOWN

    override fun runCommand(message: Message, args: List<String>): Mono<CommandResult> {
        return Bot.getRecentImage(message).flatMap { url ->
            Mono.just(if (url != null)
                ImageHelper.downloadTempFile(url) ?: CommandResult.fail("i couldnt download the image ${Bot.sadEmoji()}")
            else
                ImageHelper.createTempFile(Repost.getImageRepost(message.guild.block()!!))
                        ?: CommandResult.fail("i searched far and wide and couldnt find a picture to put your meme on ${Bot.sadEmoji()}"))
        }.flatMap { file ->
            if (file is File) {
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
            }
            Mono.just(file)
        }.flatMap {
            if (it is File) FzzyGuild.getGuild(message.guild.block()!!.id).sendVoteAttachment(it, message.channel.block()!!, message.author.get())
            Mono.just(CommandResult.success())
        }
    }
}