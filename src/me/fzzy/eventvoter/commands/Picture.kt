package me.fzzy.eventvoter.commands

import magick.CompositeOperator
import magick.ImageInfo
import magick.MagickImage
import me.fzzy.eventvoter.*
import me.fzzy.eventvoter.thread.ImageProcessTask
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.io.File
import java.net.URL
import javax.imageio.ImageIO

class Picture : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = true
    override val description = "Inserts an image into another, use -picturetypes to see all the picture types"
    override val usageText: String = "-picture <pictureType> [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {

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
        if (picture == null) {
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "That picture doesn't exist! use -picturetypes to view all the types") }
        } else {

            // Find an image from the last 10 messages sent in this channel, include the one the user sent
            val history = event.channel.getMessageHistory(10).toMutableList()
            history.add(0, event.message)
            val url: URL? = ImageFuncs.getFirstImage(history)
            if (url == null) {
                RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Couldn't find an image in the last 10 messages sent in this channel!") }
            } else {

                // Add the process to the queue
                imageProcessQueue.addToQueue(ProcessImagePicture({
                    val file = ImageFuncs.downloadTempFile(url)

                    if (file != null) {
                        val info = ImageInfo(file.name)
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

                        val give = 40
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
                    }
                    file
                }, event.channel))
            }
        }
    }

    private fun distance(coord1: Pair<Int, Int>, coord2: Pair<Int, Int>): Int {
        return Math.sqrt(Math.pow((coord2.first - coord1.first).toDouble(), 2.0) + Math.pow((coord2.second - coord1.second).toDouble(), 2.0)).toInt()
    }
}

private class ProcessImagePicture(override val code: () -> Any?, private var channel: IChannel) : ImageProcessTask {

    var processingMessage: IMessage? = null

    override fun queueUpdated(position: Int) {
        val msg = if (position == 0) "processing..." else "position in queue: $position"
        RequestBuffer.request {
            if (processingMessage == null)
                processingMessage = Funcs.sendMessage(channel, msg)
            else
                processingMessage?.edit(msg)
        }
    }

    override fun finished(obj: Any?) {
        if (obj == null) {
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, channel, "No faces detected in image.") }
        } else {
            if (obj is File) {
                RequestBuffer.request {
                    processingMessage?.delete()
                    messageScheduler.sendTempFile(60 * 1000, channel, obj)
                    obj.delete()
                }
            }
        }
    }
}