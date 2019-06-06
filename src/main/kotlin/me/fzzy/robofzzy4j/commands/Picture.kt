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
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object Picture : Command {

    override val cooldownMillis: Long = 60 * 1000 * 3
    override val votes: Boolean = false
    override val description = "Inserts an image into another, use -picturetypes to see all the picture types"
    override val usageText: String = "picture <pictureType>"
    override val allowDM: Boolean = true
    override val price: Int = 1
    override val cost: CommandCost = CommandCost.COOLDOWN

    override fun runCommand(message: IMessage, args: List<String>): CommandResult {

        // Find the specified picture from the pictures folder
        val pictureFile = File("pictures")
        var picture: File? = null
        if (args.isNotEmpty() && args[0].toLowerCase() != "random") {
            for (files in pictureFile.listFiles()) {
                if (files.nameWithoutExtension.toLowerCase() == args[0].toLowerCase()) {
                    picture = files
                    break
                }
            }
        } else picture = pictureFile.listFiles()[Bot.random.nextInt(pictureFile.listFiles().count())]
        if (picture == null)
            return CommandResult.fail("i dont know what picture that is, all the ones i know are in -picturetypes")

        // Find an image from the last 10 messages sent in this channel, include the one the user sent
        val history = message.channel.getMessageHistory(10).toMutableList()
        history.add(0, message)

        val url = ImageHelper.getFirstImage(history)
        val file = if (url == null || (args.count() == 1 && args[0].toLowerCase() == "random"))
            ImageHelper.createTempFile(Repost.getImageRepost(message.guild)) ?: return CommandResult.fail("i searched far and wide and couldnt find a picture to put your meme on :(")
        else
            ImageHelper.downloadTempFile(url) ?: return CommandResult.fail("i couldnt download the image")

        val bufferedImage = ImageIO.read(picture)

        val topRight = detectTopRightCorner(bufferedImage)
        val topLeft = detectTopLeftCorner(bufferedImage)
        val bottomRight = detectBottomRightCorner(bufferedImage)
        val bottomLeft = detectBottomLeftCorner(bufferedImage)

        // Get how much to rotate the image in radians
        val leftAverage = average(topLeft, bottomLeft)
        val rightAverage = average(topRight, bottomRight)
        val topAverage = average(topLeft, topRight)
        val bottomAverage = average(bottomLeft, bottomRight)
        val t = -Math.atan2(leftAverage.second - rightAverage.second.toDouble(), rightAverage.first - leftAverage.first.toDouble())

        // Detecting the proper width and height
        val maxLength = Math.max(distance(topRight, topLeft), distance(bottomRight, bottomLeft))
        val maxHeight = Math.max(distance(bottomRight, topRight), distance(bottomLeft, topLeft))
        val widthCross = Math.max(angleAdjust(bottomRight, -t).first - angleAdjust(topLeft, -t).first, angleAdjust(topRight, -t).first - angleAdjust(bottomLeft, -t).first)
        val heightCross = Math.max(angleAdjust(bottomLeft, -t).second - angleAdjust(topRight, -t).second, angleAdjust(bottomRight, -t).second - angleAdjust(topLeft, -t).second)
        val width = Math.max(maxLength, widthCross)
        val height = Math.max(maxHeight, heightCross)

        val composite = ImageMagickCmd("convert")
        val operation = IMOperation()

        operation.compose("dstover")
        operation.addImage(picture.absolutePath)

        operation.openOperation()

        operation.addImage(file.absolutePath)

        operation.resize(width, height, "!")
        operation.gravity("center")

        val trueAverage = average(topAverage, bottomAverage)
        val xOffset = trueAverage.first - (bufferedImage.width / 2)
        val yOffset = trueAverage.second - (bufferedImage.height / 2)
        operation.addRawArgs("-geometry", "${if (xOffset >= 0) "+$xOffset" else "$xOffset"}${if (yOffset >= 0) "+$yOffset" else "$yOffset"}")
        operation.rotate(Math.toDegrees(t))

        operation.closeOperation()

        operation.composite()

        // use this for debugging

        /*operation.draw("fill none stroke red polygon " +
                "${topLeft.first},${topLeft.second} " +
                "${bottomLeft.first},${bottomLeft.second} " +
                "${bottomRight.first},${bottomRight.second} " +
                "${topRight.first},${topRight.second}")

        operation.draw("fill none stroke blue polygon " +
                "${topAverage.first},${topAverage.second} " +
                "${leftAverage.first},${leftAverage.second} " +
                "${bottomAverage.first},${bottomAverage.second} " +
                "${rightAverage.first},${rightAverage.second}")*/

        operation.addImage(file.absolutePath)

        composite.run(operation)

        RequestBuffer.request {
            Guild.getGuild(message.guild).sendVoteAttachment(file, message.channel, message.author)
            file.delete()
        }

        return CommandResult.success()
    }

    private fun detectTopRightCorner(img: BufferedImage): Pair<Int, Int> {
        var minTrans = img.width + img.height
        var finalPixels = arrayListOf<Pair<Int, Int>>()
        for (x in -(img.height - 1) until img.width) {
            val transPixels = arrayListOf<Pair<Int, Int>>()
            for (y in 0 until img.height) {
                if (x + y < 0 || x + y >= img.width) continue
                val color = Color(img.getRGB(x + y, y), true)
                if (color.alpha != 255) transPixels.add(Pair(x + y, y))
            }
            if (transPixels.count() == 0) continue
            if (transPixels.count() > minTrans) minTrans = img.width + img.height
            if (transPixels.count() <= minTrans) {
                minTrans = transPixels.count()
                finalPixels = transPixels
            }
        }
        var x = 0
        var y = 0
        for (coord in finalPixels) {
            x += coord.first
            y += coord.second
        }
        return Pair(x / finalPixels.count(), y / finalPixels.count())
    }

    private fun detectBottomRightCorner(img: BufferedImage): Pair<Int, Int> {
        var minTrans = img.width + img.height
        var finalPixels = arrayListOf<Pair<Int, Int>>()
        for (x in 0..(img.width - 1) + (img.height - 1)) {
            val transPixels = arrayListOf<Pair<Int, Int>>()
            for (y in 0 until img.height) {
                if (x - y < 0 || x - y >= img.width) continue
                val color = Color(img.getRGB(x - y, y), true)
                if (color.alpha != 255) transPixels.add(Pair(x - y, y))
            }
            if (transPixels.count() == 0) continue
            if (transPixels.count() > minTrans) minTrans = img.width + img.height
            if (transPixels.count() <= minTrans) {
                minTrans = transPixels.count()
                finalPixels = transPixels
            }
        }
        var x = 0
        var y = 0
        for (coord in finalPixels) {
            x += coord.first
            y += coord.second
        }
        return Pair(x / finalPixels.count(), y / finalPixels.count())
    }

    private fun detectTopLeftCorner(img: BufferedImage): Pair<Int, Int> {
        var minTrans = img.width + img.height
        var finalPixels = arrayListOf<Pair<Int, Int>>()
        for (x in (img.width - 1) + (img.height - 1) downTo 0) {
            val transPixels = arrayListOf<Pair<Int, Int>>()
            for (y in 0 until img.height) {
                if (x - y < 0 || x - y >= img.width) continue
                val color = Color(img.getRGB(x - y, y), true)
                if (color.alpha != 255) transPixels.add(Pair(x - y, y))
            }
            if (transPixels.count() == 0) continue
            if (transPixels.count() > minTrans) minTrans = img.width + img.height
            if (transPixels.count() <= minTrans) {
                minTrans = transPixels.count()
                finalPixels = transPixels
            }
        }
        var x = 0
        var y = 0
        for (coord in finalPixels) {
            x += coord.first
            y += coord.second
        }
        return Pair(x / finalPixels.count(), y / finalPixels.count())
    }

    private fun detectBottomLeftCorner(img: BufferedImage): Pair<Int, Int> {
        var minTrans = img.width + img.height
        var finalPixels = arrayListOf<Pair<Int, Int>>()
        for (x in (img.width - 1) downTo -(img.height - 1)) {
            val transPixels = arrayListOf<Pair<Int, Int>>()
            for (y in 0 until img.height) {
                if (x + y < 0 || x + y >= img.width) continue
                val color = Color(img.getRGB(x + y, y), true)
                if (color.alpha != 255) transPixels.add(Pair(x + y, y))
            }
            if (transPixels.count() == 0) continue
            if (transPixels.count() > minTrans) minTrans = img.width + img.height
            if (transPixels.count() <= minTrans) {
                minTrans = transPixels.count()
                finalPixels = transPixels
            }
        }
        var x = 0
        var y = 0
        for (coord in finalPixels) {
            x += coord.first
            y += coord.second
        }
        return Pair(x / finalPixels.count(), y / finalPixels.count())
    }

    private fun distance(coord1: Pair<Int, Int>, coord2: Pair<Int, Int>): Int {
        return Math.sqrt(Math.pow((coord2.first - coord1.first).toDouble(), 2.0) + Math.pow((coord2.second - coord1.second).toDouble(), 2.0)).toInt()
    }

    private fun average(coord1: Pair<Int, Int>, coord2: Pair<Int, Int>): Pair<Int, Int> {
        return Pair(Math.round((coord1.first + coord2.first) / 2f), Math.round((coord1.second + coord2.second) / 2f))
    }

    private fun angleAdjust(coord: Pair<Int, Int>, angle: Double): Pair<Int, Int> {
        val x = coord.first.toDouble()
        val y = coord.second.toDouble()
        val r = Math.sqrt(Math.pow(x, 2.0) + Math.pow(y, 2.0))
        var t = Math.atan2(y, x)
        t += angle
        return Pair(Math.round(r * Math.cos(t)).toInt(), Math.round(r * Math.sin(t)).toInt())
    }
}