package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.thread.ImageProcessTask
import me.fzzy.robofzzy4j.*
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer

class Mc : Command {

    override val cooldownMillis: Long = 6 * 1000
    override val attemptDelete: Boolean = true
    override val description = "Generates a minecraft achievement"
    override val usageText: String = "-mc <text>"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        if (args.isNotEmpty()) {
            var achieve = ""
            for (text in args) {
                achieve += "+$text"
            }
            achieve = achieve.substring(1)
            val url = ImageFuncs.getMinecraftAchievement(achieve)
            imageProcessQueue.addToQueue(object: ImageProcessTask {

                var processingMessage: IMessage? = null

                override fun run(): Any? {
                    val file = ImageFuncs.downloadTempFile(url)
                    if (file == null) {
                        RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Couldn't contact API!") }
                    } else {
                        RequestBuffer.request {
                            processingMessage?.delete()
                            Funcs.sendFile(event.channel, file)
                            file.delete()
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
        } else {
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Invalid command syntax! $usageText") }
        }
    }
}