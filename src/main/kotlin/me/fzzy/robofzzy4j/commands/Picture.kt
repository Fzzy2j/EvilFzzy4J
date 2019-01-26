package me.fzzy.robofzzy4j.commands

import magick.CompositeOperator
import magick.ImageInfo
import magick.MagickImage
import me.fzzy.robofzzy4j.*
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.io.File
import java.net.URL
import javax.imageio.ImageIO

object Picture : Command {

    override val cooldownCategory = "image"
    override val cooldownMillis: Long = 60 * 1000 * 3
    override val votes: Boolean = false
    override val description = "Inserts an image into another, use -picturetypes to see all the picture types"
    override val usageText: String = "-picture <pictureType> [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {

        // Find the specified picture from the pictures folder
        val pictureFile = File("pictures")
        var picture: File? = null
        if (args.isNotEmpty()) {
            for (files in pictureFile.listFiles()) {
                if (files.nameWithoutExtension.toLowerCase() == args[0].toLowerCase()) {
                    picture = files
                    break
                }
            }
        }
        if (picture == null)
            return CommandResult.fail("i dont know what picture that is, all the ones i know are in -picturetypes")

        // Find an image from the last 10 messages sent in this channel, include the one the user sent
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)

        val url: URL = ImageFuncs.getFirstImage(history) ?: return CommandResult.fail("i couldnt find an image in the last 10 messages")
        val file = ImageFuncs.downloadTempFile(url) ?: return CommandResult.fail("i couldnt download that image")

        val info = ImageInfo(file.absolutePath)
        var magickImage = MagickImage(info)

        val pictureInfo = ImageInfo(picture.absolutePath)
        val pictureMagickImage = MagickImage(pictureInfo)
        val sizeHelper = ImageIO.read(picture)

        // Hard to understand, this gets the corners of the transparent box in the image
        var farRight = Pair(0, 0)
        var farLeft = Pair(sizeHelper.width, 0)
        var farTop = Pair(0, sizeHelper.height)
        var farBottom = Pair(0, 0)
        for (x in 0 until sizeHelper.width) {
            for (y in 0 until sizeHelper.height) {
                if (pictureMagickImage.getOnePixel(x, y).opacity != 0) {
                    // top right bias
                    if (farRight.first < x)
                        farRight = Pair(x, y)

                    // bottom left bias
                    if (farLeft.first >= x)
                        farLeft = Pair(x, y)

                    // top left bias
                    if (farTop.second > y)
                        farTop = Pair(x, y)

                    // bottom right bias
                    if (farBottom.second <= y)
                        farBottom = Pair(x, y)
                }
            }
        }
        // Wow this is hard to understand
        val topRight = if (farTop.first < farBottom.first) farRight else farTop
        val topLeft = if (farTop.first > farBottom.first) farLeft else farTop
        val bottomRight = if (farRight.second > farLeft.second) farRight else farBottom
        val bottomLeft = if (farRight.second < farLeft.second) farLeft else farBottom

        // Get how much to rotate the image in radians
        var t = Math.atan(Math.abs(topRight.second - topLeft.second) / Math.abs(topRight.first - topLeft.first.toDouble()))
        if (farTop.first >= farBottom.first)
            t = -t

        val give = 30
        // Get the size the image should be
        val width = Math.max(distance(topRight, topLeft), distance(bottomRight, bottomLeft)) + give
        val height = Math.max(distance(topRight, bottomRight), distance(topLeft, bottomLeft)) + give

        // Apply the transform
        magickImage = magickImage.scaleImage(width, height)
        magickImage = magickImage.rotateImage(Math.toDegrees(t))

        // Do some layering
        val og = MagickImage(pictureInfo)
        pictureMagickImage.compositeImage(CompositeOperator.OverCompositeOp, magickImage, Math.min(topLeft.first, bottomLeft.first) - give / 2, Math.min(topLeft.second, topRight.second) - give / 2)
        pictureMagickImage.compositeImage(CompositeOperator.OverCompositeOp, og, 0, 0)

        pictureMagickImage.fileName = file.absolutePath
        pictureMagickImage.writeImage(info)
        RequestBuffer.request {
            Funcs.sendFile(event.channel, file)
            file.delete()
        }

        return CommandResult.success()
    }

    private fun distance(coord1: Pair<Int, Int>, coord2: Pair<Int, Int>): Int {
        return Math.sqrt(Math.pow((coord2.first - coord1.first).toDouble(), 2.0) + Math.pow((coord2.second - coord1.second).toDouble(), 2.0)).toInt()
    }
}