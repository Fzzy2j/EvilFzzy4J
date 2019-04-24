package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.Guild
import me.fzzy.robofzzy4j.ImageHelper
import me.fzzy.robofzzy4j.util.CommandResult
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.net.URL
import java.util.*

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
        val url = getMinecraftAchievement(achieve)
        val file = ImageHelper.downloadTempFile(url) ?: return CommandResult.fail("the api didnt like that")

        RequestBuffer.request {
            Guild.getGuild(message.guild).sendVoteAttachment(file, message.channel, message.author)
            file.delete()
        }
        return CommandResult.success()
    }

    fun getMinecraftAchievement(text: String): URL {
        val url = "https://mcgen.herokuapp.com/a.php?i=${Random().nextInt(20) + 1}&h=Achievement+get!&t=$text"
        return URL(url)
    }
}