package me.fzzy.evilfzzy4j.command.image

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.io.File

object Repost : Command("repost") {

    override val cooldownMillis: Long = 10 * 1000
    override val description = "Shows you upvoted posts from the server"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = false

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {

        val repost = getRepost(event.guild) ?: return CommandResult.fail("there havent been any worthy posts in this server ${Bot.surprisedEmote.asMention}")

        event.textChannel.sendFile(repost).queue()
        repost.delete()
        return CommandResult.success()
    }

    fun getRepost(guild: Guild): File? {
        val files = File("memes", guild.id).listFiles()
        if (files == null || files.isEmpty())
            return null
        return files[Bot.random.nextInt(files.size)]
    }

    fun getImageRepost(guild: Guild): File? {
        val files = File("memes", guild.id).listFiles()
        if (files == null || files.isEmpty())
            return null

        val imgs = arrayListOf<File>()
        for (file in files) {
            if (file.extension == "jpg" || file.extension == "png") imgs.add(file)
        }
        return imgs[Bot.random.nextInt(imgs.size)]
    }
}