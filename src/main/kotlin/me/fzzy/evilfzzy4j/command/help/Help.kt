package me.fzzy.evilfzzy4j.command.help

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

object Help : Command("help") {

    override val cooldownMillis: Long = 4 * 1000
    override val description = "The help command"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {

        val matcher = Bot.URL_PATTERN.matcher(args.joinToString(" "))
        if (matcher.find()) {
            Bot.logger.info(args.joinToString(" ").substring(matcher.start(1), matcher.end()))
        }
        var helpMsg = "```md\n"
        for ((_, command) in commands) {
            val cost = "${command.cooldownMillis / 1000} second cooldown"
            var a = command.args.joinToString(prefix = "[", postfix = "]", separator = "] [")
            if (command.args.isEmpty()) a = ""
            helpMsg += "# ${Bot.data.BOT_PREFIX}${command.name} $a\n${command.description} : $cost\n\n"
        }
        helpMsg += "```"
        event.author.openPrivateChannel().queue { private -> private.sendMessage(helpMsg).queue() }

        return CommandResult.success()
    }

}