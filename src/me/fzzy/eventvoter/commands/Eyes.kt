package me.fzzy.eventvoter.commands

import magick.CompositeOperator
import magick.ImageInfo
import magick.MagickImage
import me.fzzy.eventvoter.*
import me.fzzy.eventvoter.thread.ImageProcessTask
import org.json.JSONObject
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.io.File
import java.net.URL
import javax.imageio.ImageIO
import kotlin.math.roundToInt


class Eyes : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = true
    override val description = "Adds different eyes to an image, use -eyetypes to see all the eye types"
    override val usageText: String = "-eyes <eyeType> [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {

        // Find the specified eyes from the eyes folder
        val eyesFile = File("eyes")
        var eyes: File? = null
        if (args.isNotEmpty()) {
            for (files in eyesFile.listFiles()) {
                if (files.nameWithoutExtension.toLowerCase().replace("_mirror", "") == args[0].toLowerCase()) {
                    eyes = files
                    break
                }
            }
        }
        if (eyes == null) {
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Those eyes don't exist! use -eyetypes to view all the types") }
        } else {

            // Find an image from the last 10 messages sent in this channel, include the one the user sent
            val history = event.channel.getMessageHistory(10).toMutableList()
            history.add(0, event.message)
            val url: URL? = ImageFuncs.getFirstImage(history)
            if (url == null) {
                RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Couldn't find an image in the last 10 messages sent in this channel!") }
            } else {

                // Add the process to the queue
                imageProcessQueue.addToQueue(object : ImageProcessTask {

                    var processingMessage: IMessage? = null

                    override fun run(): Any? {
                        val faces = ImageFuncs.getFacialInfo("", false, true, url.toString())
                        val file = ImageFuncs.downloadTempFile(url)

                        if (faces != null && file != null) {
                            val info = ImageInfo(file.name)
                            val magickImage = MagickImage(info)

                            val eyeInfo = ImageInfo(eyes.absolutePath)
                            val eyeMagickImage = MagickImage(eyeInfo)

                            val sizeHelper = ImageIO.read(eyes)
                            val ratio = ((sizeHelper.width + sizeHelper.height) / 2.0) / 250.0
                            if (faces.length() > 0) {
                                for (face in faces) {
                                    if (face is JSONObject) {
                                        val left = face.getJSONObject("faceLandmarks").getJSONObject("pupilLeft")
                                        val right = face.getJSONObject("faceLandmarks").getJSONObject("pupilRight")

                                        // The images need scaling based on how big the eyes are, this is done using the distance between the eyes
                                        val lx = left.getInt("x")
                                        val ly = left.getInt("y")
                                        val rx = right.getInt("x")
                                        val ry = right.getInt("y")
                                        var eyeDistance = Math.sqrt(Math.pow((lx - rx).toDouble(), 2.0) + Math.pow((ly - ry).toDouble(), 2.0))
                                        eyeDistance *= ratio
                                        val width = if (sizeHelper.width > eyeDistance) eyeDistance.roundToInt() else sizeHelper.width
                                        val height = if (sizeHelper.height > eyeDistance) eyeDistance.roundToInt() else sizeHelper.height
                                        var resize = eyeMagickImage.scaleImage(width, height)

                                        magickImage.compositeImage(CompositeOperator.OverCompositeOp, resize, lx - width / 2, ly - height / 2)

                                        // If the eye file ends in _mirror the other eye will be flipped
                                        if (eyes.nameWithoutExtension.endsWith("_mirror"))
                                            resize = resize.flopImage()

                                        magickImage.compositeImage(CompositeOperator.OverCompositeOp, resize, rx - width / 2, ry - height / 2)
                                    }
                                }

                                magickImage.fileName = file.absolutePath
                                magickImage.writeImage(info)
                                RequestBuffer.request {
                                    processingMessage?.delete()
                                    messageScheduler.sendTempFile(60 * 1000, event.channel, file)
                                    file.delete()
                                }
                            }
                        }
                        return file
                    }

                    override fun queueUpdated(position: Int) {
                        val msg = if (position == 0) "processing..." else "position in queue: $position"
                        RequestBuffer.request {
                            if (processingMessage == null)
                                processingMessage = Funcs.sendMessage(event.channel, msg)
                            else
                                processingMessage?.edit(msg)
                        }
                    }
                })
            }
        }
    }
}