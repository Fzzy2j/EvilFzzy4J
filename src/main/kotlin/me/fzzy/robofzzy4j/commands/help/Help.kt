package me.fzzy.robofzzy4j.commands.help

import discord4j.core.`object`.entity.Message
import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.CommandHandler
import me.fzzy.robofzzy4j.util.CommandCost
import me.fzzy.robofzzy4j.util.CommandResult
import reactor.core.publisher.Mono

object Help : Command("help") {

    override val cooldownMillis: Long = 4 * 1000
    override val votes: Boolean = false
    override val description = "The help command"
    override val args: ArrayList<String> = arrayListOf()
    override val allowDM: Boolean = true
    override val price: Int = 0
    override val cost: CommandCost = CommandCost.CURRENCY

    override fun runCommand(message: Message, args: List<String>): Mono<CommandResult> {
        var helpMsg = "```md\n"
        for ((_, command) in CommandHandler.getAllCommands()) {
            val cost = when {
                command.cost == CommandCost.COOLDOWN -> "${command.cooldownMillis / 1000} second cooldown"
                command.price > 0 -> "${command.price} ${Bot.CURRENCY_EMOJI}"
                else -> "Free"
            }
            var a = command.args.joinToString(prefix = "[", postfix = "]", separator = "] [")
            if (command.args.isEmpty()) a = ""
            helpMsg += "# ${Bot.data.BOT_PREFIX}${command.name} $a\n${command.description} : $cost\n\n"
        }
        helpMsg += "```"
        message.channel.block()!!.createMessage(helpMsg).block()

        return Mono.just(CommandResult.success())
    }

}