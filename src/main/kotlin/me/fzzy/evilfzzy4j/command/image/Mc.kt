package me.fzzy.evilfzzy4j.command.image

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import me.fzzy.evilfzzy4j.util.ImageHelper
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.net.URL
import java.util.*

object Mc : Command("mc") {

    override val cooldownMillis: Long = 10 * 1000
    override val description = "Generates a minecraft achievement"
    override val args: ArrayList<String> = arrayListOf("text")
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {

        var achieve = ""
        for (text in args) {
            achieve += "+$text"
        }
        achieve = achieve.substring(1)
        val url = getMinecraftAchievement(achieve)
        val file = ImageHelper.downloadTempFile(url) ?: return CommandResult.fail("the api didnt like that ${Bot.surprisedEmote.asMention}")

        event.textChannel.sendFile(file).queue()
        file.delete()
        return CommandResult.success()
    }

    fun getMinecraftAchievement(text: String): URL {
        val url = "https://mcgen.herokuapp.com/a.php?i=${Random().nextInt(20) + 1}&h=Achievement+get!&t=$text"
        return URL(url)
    }
}