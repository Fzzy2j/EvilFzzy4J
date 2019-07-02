package me.fzzy.robofzzy4j.command

import discord4j.core.`object`.entity.Message
import discord4j.core.event.domain.message.MessageCreateEvent
import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.FzzyGuild
import me.fzzy.robofzzy4j.FzzyUser
import reactor.core.publisher.Mono
import java.util.*
import kotlin.math.max
import kotlin.math.roundToInt

abstract class Command constructor(val name: String) {

    abstract val description: String
    abstract val votes: Boolean
    abstract val args: ArrayList<String>
    abstract val allowDM: Boolean
    abstract val cooldownMillis: Long
    abstract val price: Int
    abstract val cost: CommandCost

    abstract fun runCommand(message: Message, args: List<String>): Mono<CommandResult>

    companion object {
        val commands: HashMap<String, Command> = hashMapOf()

        fun registerCommand(string: String, command: Command) {
            commands[string.toLowerCase()] = command
        }

        fun getCommand(string: String): Command? {
            return commands[string.toLowerCase()]
        }

        fun unregisterCommand(string: String): Boolean {
            if (commands.containsKey(string)) {
                commands.remove(string)
                return true
            }
            return false
        }
    }

    fun handleCommand(event: MessageCreateEvent): Mono<CommandResult> {
        val args = event.message.content.get().split(" ")
        var argsList: List<String> = args.toMutableList()
        argsList = argsList.drop(1)

        val user = FzzyUser.getUser(event.member.get().id)

        if (!event.guildId.isPresent) {
            if (!allowDM) {
                return Mono.just(CommandResult.fail("this command is not allowed in DMs!"))
            }
        }

        Bot.logger.info("${event.member.get().displayName}#${event.member.get().discriminator} running command: ${event.message.content.get()}")
        return when (cost) {
            CommandCost.CURRENCY -> {
                val currency = if (price > 0) FzzyGuild.getGuild(event.guildId.get()).getCurrency(user) else 0
                Bot.client.applicationInfo.flatMap {
                    val currencyMsg = "${event.member.get().displayName.toLowerCase()} this command costs $price ${Bot.toUsable(Bot.currencyEmoji)}, you only have $currency ${Bot.toUsable(Bot.currencyEmoji)}"
                    if (user.id == it.ownerId || currency >= price) {
                        runCommand(user, event.message, argsList)
                    } else {
                        Mono.just(CommandResult.fail(currencyMsg))
                    }
                }
            }
            CommandCost.COOLDOWN -> {
                Bot.client.applicationInfo.flatMap {
                    val timeLeftMinutes = Math.ceil((user.cooldown.timeLeft(1.0)) / 1000.0 / 60.0).roundToInt()
                    val content = "${event.member.get().displayName.toLowerCase()} you are still on cooldown for $timeLeftMinutes minute${if (timeLeftMinutes != 1) "s" else ""}"
                    if (user.id == it.ownerId || user.cooldown.isReady(1.0)) {
                        runCommand(user, event.message, argsList)
                    } else {
                        Mono.just(CommandResult.fail(content))
                    }
                }
            }
        }
    }

    private fun runCommand(fzzyUser: FzzyUser, message: Message, argsList: List<String>): Mono<CommandResult> {
        return runCommand(message, argsList).flatMap { result ->
            if (result.isSuccess()) {
                val guild = message.guild.block()!!
                val fzzyGuild = FzzyGuild.getGuild(guild.id)
                when (cost) {
                    CommandCost.COOLDOWN -> {
                        fzzyUser.cooldown.triggerCooldown(cooldownMillis)
                    }
                    CommandCost.CURRENCY -> {
                        if (price != 0) {
                            fzzyGuild.addCurrency(fzzyUser, max(-price, -fzzyGuild.getCurrency(fzzyUser)))
                        }
                    }
                }
                if (votes)
                    fzzyGuild.allowVotes(message)
            }
            Mono.just(result)
        }
    }

}