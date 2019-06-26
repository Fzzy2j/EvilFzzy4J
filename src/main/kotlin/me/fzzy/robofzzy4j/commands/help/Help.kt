package me.fzzy.robofzzy4j.commands.help

import discord4j.core.`object`.entity.Message
import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.Command
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

        val matcher = Bot.URL_PATTERN.matcher(args.joinToString(" "))
        if (matcher.find()) {
            Bot.logger.info(args.joinToString(" ").substring(matcher.start(1), matcher.end()))
        }
        var helpMsg = "```md\n"
        for ((_, command) in commands) {
            val cost = when {
                command.cost == CommandCost.COOLDOWN -> "${command.cooldownMillis / 1000} second cooldown"
                command.price > 0 -> {
                    val custom = Bot.currencyEmoji.asCustomEmoji()
                    val name = if (custom.isPresent)
                        custom.get().name
                    else
                        Bot.currencyEmoji.asUnicodeEmoji().get().raw
                    "${command.price} $name"
                }
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