package me.fzzy.robofzzy4j.commands.help

import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.commandHandler
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.RequestBuffer

class Help : Command {

    override val cooldownMillis: Long = 4 * 1000
    override val attemptDelete: Boolean = true
    override val description = "The help command"
    override val usageText: String = "-help"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        var helpMsg = "```<> = required | [] = optional\n\n"
        for ((_, command) in commandHandler.getAllCommands()) {
            helpMsg += "${command.usageText} : ${command.cooldownMillis / 1000} seconds : ${command.description}\n"
        }
        helpMsg += "```"
        RequestBuffer.request { event.author.orCreatePMChannel.sendMessage(helpMsg) }
    }

}