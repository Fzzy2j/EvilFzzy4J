package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import org.im4java.core.CompositeCmd
import org.im4java.core.ConvertCmd
import org.im4java.core.IMOperation
import org.im4java.core.Stream2BufferedImage
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

                Thread(Runnable {
                    val faces = ImageFuncs.getFacialInfo("", false, true, url.toString())
                    val file = ImageFuncs.downloadTempFile(url)

                    if (faces != null && file != null) {

                        val sizeHelper = ImageIO.read(eyes)
                        val ratio = ((sizeHelper.width + sizeHelper.height) / 2.0) / 250.0
                        if (faces.length() > 0) {
                            var s2b = Stream2BufferedImage()
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

                                    var op = IMOperation()
                                    op.addImage()
                                    op.resize(width, height)
                                    op.addImage("jpg:-")
                                    convert.setOutputConsumer(s2b)
                                    convert.run(op, ImageIO.read(eyes))
                                    var resize = s2b.image

                                    op = IMOperation()

                                    val composite = CompositeCmd()
                                    composite.searchPath = "C:\\Program Files\\ImageMagick-7.0.8-Q16"
                                    composite.setOutputConsumer(s2b)
                                    op.gravity("+${lx - width / 2}+${ly - height / 2}")
                                    op.addImage()
                                    op.addImage()

                                    op.addImage("jpg:-")
                                    composite.run(op, resize, ImageIO.read(file))
                                    val eye1 = s2b.image
                                    //magickImage.compositeImage(CompositeOperator.OverCompositeOp, resize, lx - width / 2, ly - height / 2)

                                    // If the eye file ends in _mirror the other eye will be flipped
                                    if (eyes.nameWithoutExtension.endsWith("_mirror")) {
                                        op = IMOperation()
                                        op.flop()
                                        op.addImage()
                                        op.addImage("jpg:-")
                                        convert.run(op, resize)
                                        resize = s2b.image
                                    }

                                    op = IMOperation()

                                    op.composite()
                                    op.gravity("+${rx - width / 2}+${ry - height / 2}")
                                    op.addImage()
                                    op.addImage()
                                    op.addImage("jpg:-")
                                    convert.run(op, resize, eye1)
                                    //magickImage.compositeImage(CompositeOperator.OverCompositeOp, resize, rx - width / 2, ry - height / 2)
                                }
                            }


                            ImageIO.write(s2b.image, file.name, file)
                            RequestBuffer.request {
                                Funcs.sendFile(event.channel, file)
                                file.delete()
                            }
                        }
                    }
                }).start()
            }
        }
    }
}