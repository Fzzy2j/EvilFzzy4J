package me.fzzy.robofzzy4j.commands.help

import me.fzzy.robofzzy4j.*
import me.fzzy.robofzzy4j.util.Zalgo
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.RequestBuffer

class Help : Command {

    override val cooldownMillis: Long = 4 * 1000
    override val votes: Boolean = false
    override val description = "The help command"
    override val usageText: String = "-help"
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        var helpMsg = "```md\n# <> = required | [] = optional\n\n"
        for ((_, command) in commandHandler.getAllCommands()) {
            helpMsg += "${command.usageText} : ${command.cooldownMillis / 1000} seconds : ${command.description}\n"
        }
        //if (cli.getGuildByID(MEME_SERVER_ID).users.contains(event.author))
            //helpMsg += "# -code : ${Zalgo.goZalgo("Try me", false, true, false, false, true)}"
        helpMsg += "```"
        RequestBuffer.request { event.author.orCreatePMChannel.sendMessage(helpMsg) }
        return CommandResult.success()
    }

}