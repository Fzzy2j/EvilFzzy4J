package me.fzzy.evilfzzy4j.command

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import me.fzzy.evilfzzy4j.FzzyUser
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
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

    abstract fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult

    companion object : ListenerAdapter() {
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

        override fun onMessageReceived(event: MessageReceivedEvent) {
            if (!event.message.contentRaw.startsWith(Bot.data.BOT_PREFIX)) return
            for ((cmdName, cmd) in commands) {
                if (event.message.contentRaw.toLowerCase().substring(1).startsWith(cmdName)) {
                    Bot.scheduler.schedule { cmd.handleCommand(event, event.channel.latestMessageIdLong) }
                    break
                }
            }
        }
    }

    fun handleCommand(event: MessageReceivedEvent, latestMessageId: Long) {
        var args = event.message.contentRaw.split(" ")
        args = args.drop(1)

        val user = FzzyUser.getUser(event.author.idLong)

        if (!event.isFromGuild) {
            if (!allowDM || cost == CommandCost.CURRENCY) {
                event.channel.sendMessage("this command is not allowed in DMs!").queue()
                return
            }
        }

        Bot.logger.info("${event.author.name}#${event.author.discriminator} running command: ${event.message.contentRaw}")

        if (!votes) delete(event.message)
        else if (event.message.isFromGuild) FzzyGuild.getGuild(event.guild.id).allowVotes(event.message)

        when (cost) {
            CommandCost.CURRENCY -> {
                val currency = if (price > 0) FzzyGuild.getGuild(event.guild.id).getCurrency(user) else 0
                val name = (event.member!!.effectiveName).toLowerCase()
                val currencyMsg = "$name this command costs $price ${Bot.currencyEmoji.asMention}, you only have $currency ${Bot.currencyEmoji.asMention}"
                val info = Bot.client.retrieveApplicationInfo().complete()
                if (user.id == info.owner.idLong || currency >= price) {
                    val result = runCommand(event, args, latestMessageId)
                    if (result.isSuccess()) {
                        if (price != 0) {
                            val guild = FzzyGuild.getGuild(event.guild.id)
                            guild.addCurrency(user, max(-price, -guild.getCurrency(user)))
                        } else if (result.getMessage() != null) {
                            event.channel.sendMessage(result.getMessage()!!).queue { msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES) }
                        }
                    }
                } else {
                    event.channel.sendMessage(currencyMsg).queue { msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES) }
                }
            }
            CommandCost.COOLDOWN -> {
                val timeLeftMinutes = ceil((user.cooldown.timeLeft(1.0)) / 1000.0 / 60.0).roundToInt()
                val content = "${event.author.name} you are still on cooldown for $timeLeftMinutes minute${if (timeLeftMinutes != 1) "s" else ""}"
                val info = Bot.client.retrieveApplicationInfo().complete()
                if (user.id == info.owner.idLong || user.cooldown.isReady(1.0)) {
                    user.cooldown.triggerCooldown(cooldownMillis)
                    val result = runCommand(event, args, latestMessageId)
                    if (!result.isSuccess() && result.getMessage() != null) {
                        event.channel.sendMessage(result.getMessage()!!).queue { msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES) }
                    }
                } else {
                    event.channel.sendMessage(content).queue { msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES) }
                }
            }
        }
    }

    private fun delete(msg: Message) {
        if (msg.attachments.count() == 0 && msg.isFromGuild) {
            msg.delete().queue()
        }
    }

}