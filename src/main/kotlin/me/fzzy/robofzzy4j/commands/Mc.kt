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

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        if (args.isEmpty())
            return CommandResult.fail("Invalid command syntax! $usageText")

        var achieve = ""
        for (text in args) {
            achieve += "+$text"
        }
        achieve = achieve.substring(1)
        val url = ImageFuncs.getMinecraftAchievement(achieve)
        val file = ImageFuncs.downloadTempFile(url) ?: return CommandResult.fail("Couldn't contact API!")

        RequestBuffer.request {
            Funcs.sendFile(event.channel, file)
            file.delete()
        }
        return CommandResult.success()
    }
}