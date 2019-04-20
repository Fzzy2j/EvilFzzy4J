package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer

object Mc : Command {

    override val cooldownCategory = "image"
    override val cooldownMillis: Long = 60 * 1000
    override val votes: Boolean = false
    override val description = "Generates a minecraft achievement"
    override val usageText: String = "mc <text>"
    override val allowDM: Boolean = true
    override val cost: Int = 1

    override fun runCommand(message: IMessage, args: List<String>): CommandResult {
        if (args.isEmpty())
            return CommandResult.fail("hello? $usageText")

        var achieve = ""
        for (text in args) {
            achieve += "+$text"
        }
        achieve = achieve.substring(1)
        val url = ImageFuncs.getMinecraftAchievement(achieve)
        val file = ImageFuncs.downloadTempFile(url) ?: return CommandResult.fail("the api didnt like that")

        RequestBuffer.request {
            Funcs.sendFile(message.channel, file)
            file.delete()
        }
        return CommandResult.success()
    }
}