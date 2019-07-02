package me.fzzy.robofzzy4j.command.image

import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.FzzyGuild
import me.fzzy.robofzzy4j.command.Command
import me.fzzy.robofzzy4j.command.CommandCost
import me.fzzy.robofzzy4j.command.CommandResult
import reactor.core.publisher.Mono
import java.io.File

object Repost : Command("repost") {

    override val cooldownMillis: Long = 60 * 1000 * 5
    override val votes: Boolean = false
    override val description = "Shows you upvoted posts from the server"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = false
    override val price: Int = 1
    override val cost: CommandCost = CommandCost.COOLDOWN

    override fun runCommand(message: Message, args: List<String>): Mono<CommandResult> {

        val repost = getRepost(message.guild.block()!!) ?: return Mono.just(CommandResult.fail("there havent been any worthy posts in this server ${Bot.surprisedEmoji()}"))

        FzzyGuild.getGuild(message.guild.block()!!.id).sendVoteAttachment(repost, message.channel.block()!!, message.author.get())
        repost.delete()
        return Mono.just(CommandResult.success())
    }

    fun getRepost(guild: Guild): File? {
        val files = File("memes", guild.id.asString()).listFiles()
        if (files == null || files.isEmpty())
            return null
        return files[Bot.random.nextInt(files.size)]
    }

    fun getImageRepost(guild: Guild): File? {
        val files = File("memes", guild.id.asString()).listFiles()
        if (files == null || files.isEmpty())
            return null

        val imgs = arrayListOf<File>()
        for (file in files) {
            if (file.extension == "jpg" || file.extension == "png") imgs.add(file)
        }
        return imgs[Bot.random.nextInt(imgs.size)]
    }
}