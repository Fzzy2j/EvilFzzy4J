package me.fzzy.eventvoter.commands

import me.fzzy.eventvoter.*
import me.fzzy.eventvoter.thread.ImageProcessTask
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.io.File

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
            imageProcessQueue.addToQueue(ProcessImageMc({ ImageFuncs.downloadTempFile(url) }, event.channel))
        } else {
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Invalid command syntax! $usageText") }
        }
    }

}

private class ProcessImageMc(override val code: () -> Any?, private var channel: IChannel) : ImageProcessTask {

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
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, channel, "Couldn't contact API!") }
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