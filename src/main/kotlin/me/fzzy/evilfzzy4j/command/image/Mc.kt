package me.fzzy.evilfzzy4j.command.image

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandCost
import me.fzzy.evilfzzy4j.command.CommandResult
import me.fzzy.evilfzzy4j.util.ImageHelper
import reactor.core.publisher.Mono
import java.net.URL
import java.util.*

object Mc : Command("mc") {

    override val cooldownMillis: Long = 60 * 1000
    override val votes: Boolean = false
    override val description = "Generates a minecraft achievement"
    override val args: ArrayList<String> = arrayListOf("text")
    override val allowDM: Boolean = true
    override val price: Int = 1
    override val cost: CommandCost = CommandCost.COOLDOWN

    override fun runCommand(message: CachedMessage, args: List<String>): Mono<CommandResult> {

        var achieve = ""
        for (text in args) {
            achieve += "+$text"
        }
        achieve = achieve.substring(1)
        val url = getMinecraftAchievement(achieve)
        val file = ImageHelper.downloadTempFile(url) ?: return Mono.just(CommandResult.fail("the api didnt like that ${Bot.surprisedEmoji()}"))


        FzzyGuild.getGuild(message.guild.id).sendVoteAttachment(file, message.channel, message.author)
        file.delete()
        return Mono.just(CommandResult.success())
    }

    fun getMinecraftAchievement(text: String): URL {
        val url = "https://mcgen.herokuapp.com/a.php?i=${Random().nextInt(20) + 1}&h=Achievement+get!&t=$text"
        return URL(url)
    }
}