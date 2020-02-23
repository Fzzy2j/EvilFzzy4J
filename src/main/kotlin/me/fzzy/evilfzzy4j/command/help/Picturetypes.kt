package me.fzzy.evilfzzy4j.command.help

import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.io.File

object Picturetypes : Command("picturetypes") {

    override val cooldownMillis: Long = 4 * 1000
    override val description: String = "Shows all the picturess the bot can insert an image into using the -picture command"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {
        var all = "```"
        for (file in File("pictures").listFiles()!!) {
            all += "-picture ${file.nameWithoutExtension}\n"
        }
        all += "-picture random"
        all += "```"
        event.author.openPrivateChannel().queue { private ->
            run {
                private.sendMessage(all).queue()
            }
        }
        return CommandResult.success()
    }

}