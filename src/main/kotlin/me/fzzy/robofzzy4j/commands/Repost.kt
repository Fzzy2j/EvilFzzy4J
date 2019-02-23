package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.util.RequestBuffer
import java.io.File

object Repost : Command {

    override val cooldownCategory = "repost"
    override val cooldownMillis: Long = 60 * 1000 * 5
    override val votes: Boolean = false
    override val description = "Shows you upvoted posts from the server"
    override val usageText: String = "-repost"
    override val allowDM: Boolean = false

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {

        val repost = getRepost(event.guild)?: return CommandResult.fail("there havent been any worthy posts in this server, sorry")

        RequestBuffer.request {
            Funcs.sendFile(event.channel, repost, false)
            repost.delete()
        }
        return CommandResult.success()
    }

    fun getRepost(guild: IGuild): File? {
        val files = File("memes", guild.longID.toString()).listFiles()
        if (files == null || files.isEmpty())
            return null
        return files[RoboFzzy.random.nextInt(files.size)]
    }

    fun getImageRepost(guild: IGuild): File? {
        val files = File("memes", guild.longID.toString()).listFiles()
        if (files == null || files.isEmpty())
            return null

        val imgs = arrayListOf<File>()
        for (file in files) {
            if (file.extension == "jpg" || file.extension == "png") imgs.add(file)
        }
        return imgs[RoboFzzy.random.nextInt(imgs.size)]
    }
}