package me.fzzy.robofzzy4j.commands

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
            Thread(Runnable {
                val file = ImageFuncs.downloadTempFile(url)
                if (file == null) {
                    RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Couldn't contact API!") }
                } else {
                    RequestBuffer.request {
                        Funcs.sendFile(event.channel, file)
                        file.delete()
                    }
                }
            }).start()
        } else {
            RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "Invalid command syntax! $usageText") }
        }
    }
}