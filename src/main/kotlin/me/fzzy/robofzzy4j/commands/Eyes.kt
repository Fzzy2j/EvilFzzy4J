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

//
// UNUSED
//

object Eyes : Command {

    override val cooldownCategory = "image"
    override val cooldownMillis: Long = 60 * 1000 * 3
    override val votes: Boolean = false
    override val description = "Adds different eyes to an image, use -eyetypes to see all the eye types"
    override val usageText: String = "-eyes <eyeType> [imageUrl]"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {

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
        if (eyes == null)
            return CommandResult.fail("i dont have eyes with that name, ill tell you which ones i know if you type -eyetypes")

        // Find an image from the last 10 messages sent in this channel, include the one the user sent
        val history = event.channel.getMessageHistory(10).toMutableList()
        history.add(0, event.message)

        val url: URL = ImageFuncs.getFirstImage(history)
                ?: return CommandResult.fail("i couldnt find an image in the last 10 messages")

        val faces = ImageFuncs.getFacialInfo("", false, true, url.toString())
        val file = ImageFuncs.downloadTempFile(url) ?: return CommandResult.fail("i couldnt download the image")

        if (faces == null || faces.length() == 0)
            return CommandResult.fail("the api couldnt find any faces in that picture")

        //val info = ImageInfo(file.absolutePath)
        //val magickImage = MagickImage(info)

        //val eyeInfo = ImageInfo(eyes.absolutePath)
        //val eyeMagickImage = MagickImage(eyeInfo)

        val sizeHelper = ImageIO.read(eyes)
        val ratio = ((sizeHelper.width + sizeHelper.height) / 2.0) / 250.0
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
                //var resize = eyeMagickImage.scaleImage(width, height)

                //magickImage.compositeImage(CompositeOperator.OverCompositeOp, resize, lx - width / 2, ly - height / 2)

                // If the eye file ends in _mirror the other eye will be flipped
                //if (eyes.nameWithoutExtension.endsWith("_mirror"))
                    //resize = resize.flopImage()

                //magickImage.compositeImage(CompositeOperator.OverCompositeOp, resize, rx - width / 2, ry - height / 2)
            }
        }

        //magickImage.fileName = file.absolutePath
        //magickImage.writeImage(info)
        RequestBuffer.request {
            Funcs.sendFile(event.channel, file)
            file.delete()
        }
        return CommandResult.success()
    }
}