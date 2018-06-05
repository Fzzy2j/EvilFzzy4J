package me.fzzy.eventvoter

import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

interface Command {
    fun runCommand(event: MessageReceivedEvent, args: List<String>)
}