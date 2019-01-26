package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.CommandResult
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

object Epic : Command {

    override val cooldownCategory = "image"
    override val cooldownMillis: Long = 1000 * 60 * 10
    override val description: String = "Plays something epic in your voice channel, use -sounds to see all the sounds"
    override val votes: Boolean = true
    override val usageText: String = "-epic [sound]"
    override val allowDM: Boolean = false

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}