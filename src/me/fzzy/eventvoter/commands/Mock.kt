package me.fzzy.eventvoter.commands

import magick.DrawInfo
import magick.ImageInfo
import magick.MagickImage
import magick.PixelPacket
import me.fzzy.eventvoter.*
import me.fzzy.eventvoter.thread.ImageProcessTask
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.awt.Font
import java.awt.font.FontRenderContext
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO
import kotlin.math.roundToInt


class Mock : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = true
    override val description = "Generates an image that mocks the last message sent by another user"
    override val usageText: String = "-mock <mock>"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {

        // Find the specified eyes from the eyes folder
        val mockFile = File("mock")
        var mock: File? = null
        if (args.isNotEmpty()) {
            for (files in mockFile.listFiles()) {
                if (files.nameWithoutExtension.toLowerCase() == args[0].toLowerCase()) {
                    mock = files
                    break
                }
            }
        }
        if (mock == null) {
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "That mock base doesn't exist! use -mocks to view all the types") }
        } else {

            // Find an image from the last 10 messages sent in this channel, include the one the user sent
            var text: String? = null
            for (message in event.channel.getMessageHistory(10)) {
                if (message.author.longID != event.author.longID) {
                    if (message.content.length in 0..100) {
                        text = message.content
                        break
                    }
                }
            }
            if (text == null) {
                RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Couldn't find text to mock!") }
            } else {
                // Add the process to the queue
                imageProcessQueue.addToQueue(object : ImageProcessTask {

                    var processingMessage: IMessage? = null

                    override fun run(): Any? {
                        val r = Random()
                        val info = ImageInfo(mock.absolutePath)
                        val magickImage = MagickImage(info)
                        val sizeHelper = ImageIO.read(mock)

                        var ruin = ""
                        for (i in 0 until text.length) {
                            ruin += if (r.nextInt(2) == 0) text[i].toUpperCase() else text[i].toLowerCase()
                        }

                        annotateCenter(sizeHelper, magickImage, info, ruin, true)

                        val output = File("temp.jpg")
                        magickImage.fileName = output.absolutePath
                        magickImage.writeImage(info)
                        RequestBuffer.request {
                            processingMessage?.delete()
                            Funcs.sendFile(event.channel, output)
                            output.delete()
                        }
                        return null
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

    fun annotateCenter(sizeHelper: BufferedImage, magickImage: MagickImage, info: ImageInfo, text: String, bottom: Boolean) {
        val aInfo = DrawInfo(info)
        aInfo.fill = PixelPacket.queryColorDatabase("white")
        aInfo.stroke = PixelPacket.queryColorDatabase("black")
        aInfo.strokeAntialias = true
        aInfo.strokeWidth = 2.0
        aInfo.opacity = 100
        aInfo.pointsize = 1.0
        aInfo.font = "Impact"
        aInfo.text = text
        val affinetransform = AffineTransform()
        val frc = FontRenderContext(affinetransform, true, true)
        var font = Font(aInfo.font, Font.PLAIN, aInfo.pointsize.toInt())
        var textWidth = font.getStringBounds(text, frc).width
        while (textWidth < sizeHelper.width * 0.75) {
            aInfo.pointsize += 1
            font = Font(aInfo.font, Font.PLAIN, aInfo.pointsize.toInt())
            textWidth = font.getStringBounds(text, frc).width
        }

        val yOffset = if (bottom) sizeHelper.height - 30 else aInfo.pointsize.roundToInt() + 20
        aInfo.geometry = "+${(sizeHelper.width / 2) - (textWidth / 2)}+$yOffset"

        magickImage.annotateImage(aInfo)
    }
}