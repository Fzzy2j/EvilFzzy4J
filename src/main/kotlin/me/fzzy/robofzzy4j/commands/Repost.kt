package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.FzzyGuild
import me.fzzy.robofzzy4j.util.CommandCost
import me.fzzy.robofzzy4j.util.CommandResult
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import java.io.File

object Repost : Command("repost") {

    override val cooldownMillis: Long = 60 * 1000 * 5
    override val votes: Boolean = false
    override val description = "Shows you upvoted posts from the server"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = false
    override val price: Int = 1
    override val cost: CommandCost = CommandCost.COOLDOWN

    override fun runCommand(message: IMessage, args: List<String>): CommandResult {

        val repost = getRepost(message.guild)?: return CommandResult.fail("there havent been any worthy posts in this server, sorry ${Bot.SURPRISED_EMOJI}")

        RequestBuffer.request {
            FzzyGuild.getGuild(message.guild).sendVoteAttachment(repost, message.channel, message.author)
            repost.delete()
        }
        return CommandResult.success()
    }

    fun getRepost(guild: IGuild): File? {
        val files = File("memes", guild.longID.toString()).listFiles()
        if (files == null || files.isEmpty())
            return null
        return files[Bot.random.nextInt(files.size)]
    }

    fun getImageRepost(guild: IGuild): File? {
        val files = File("memes", guild.longID.toString()).listFiles()
        if (files == null || files.isEmpty())
            return null

        val imgs = arrayListOf<File>()
        for (file in files) {
            if (file.extension == "jpg" || file.extension == "png") imgs.add(file)
        }
        return imgs[Bot.random.nextInt(imgs.size)]
    }
}