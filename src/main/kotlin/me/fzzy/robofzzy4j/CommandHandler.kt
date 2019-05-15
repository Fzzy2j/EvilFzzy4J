package me.fzzy.robofzzy4j

import me.fzzy.robofzzy4j.util.CommandCost
import me.fzzy.robofzzy4j.util.CommandResult
import sx.blah.discord.Discord4J
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.util.*
import kotlin.math.roundToInt


object CommandHandler {

    private var commandMap: HashMap<String, Command> = hashMapOf()

    fun registerCommand(string: String, command: Command) {
        commandMap[string.toLowerCase()] = command
    }

    fun getCommand(string: String): Command? {
        return commandMap[string.toLowerCase()]
    }

    fun getAllCommands(): HashMap<String, Command> {
        return commandMap
    }

    fun unregisterCommand(string: String): Boolean {
        if (commandMap.containsKey(string)) {
            commandMap.remove(string)
            return true
        }
        return false
    }

    fun isCommand(string: String): Boolean {
        for (cmd in commandMap.keys) {
            if (string.toLowerCase().startsWith("-${cmd.toLowerCase()}")) return true
        }
        return false
    }

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {
        val args = event.message.content.split(" ")
        if (args.isEmpty())
            return
        if (!args[0].startsWith(Bot.data.BOT_PREFIX))
            return
        val commandString = args[0].substring(1)
        var argsList: List<String> = args.toMutableList()
        argsList = argsList.drop(1)

        val user = User.getUser(event.author)

        if (commandMap.containsKey(commandString.toLowerCase())) {

            val command = commandMap[commandString.toLowerCase()]!!

            if (event.channel.isPrivate) {
                if (!command.allowDM) {
                    RequestBuffer.request { MessageScheduler.sendTempMessage(Bot.data.DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "This command is not allowed in DMs!") }
                    return
                }
            }

            Discord4J.LOGGER.info("${event.author.name}#${event.author.discriminator} running command: ${event.message.content}")
            when (command.cost) {
                CommandCost.CURRENCY -> {
                    val currency = if (command.price > 0) Guild.getGuild(event.guild).getCurrency(user) else 0
                    if (user.id == Bot.client.applicationOwner.longID || currency >= command.price) {
                        runCommand(user, command, event.message, argsList)
                    } else {
                        tryDelete(event.message)
                        val content = "${event.author.name.toLowerCase()} this command costs ${command.price} ${Bot.CURRENCY_EMOJI}, you only have $currency ${Bot.CURRENCY_EMOJI}"
                        RequestBuffer.request { MessageScheduler.sendTempMessage(Bot.data.DEFAULT_TEMP_MESSAGE_DURATION, event.channel, content) }
                    }
                }
                CommandCost.COOLDOWN -> {
                    if (user.id == Bot.client.applicationOwner.longID || user.cooldown.isReady(1.0)) {
                        runCommand(user, command, event.message, argsList)
                    } else {
                        tryDelete(event.message)
                        val timeLeftMinutes = Math.ceil((user.cooldown.timeLeft(1.0)) / 1000.0 / 60.0).roundToInt()
                        val content = "${event.author.name.toLowerCase()} you are still on cooldown for $timeLeftMinutes minute${if (timeLeftMinutes != 1) "s" else ""}"
                        RequestBuffer.request { MessageScheduler.sendTempMessage(Bot.data.DEFAULT_TEMP_MESSAGE_DURATION, event.channel, content) }
                    }
                }
            }
        }
    }

    fun runCommand(user: User, command: Command, message: IMessage, argsList: List<String>) {
        if (!user.runningCommand) {
            user.runningCommand = true
            if (!command.votes) tryDelete(message)
            Bot.executor.submit {
                try {
                    val result = try {
                        command.runCommand(message, argsList)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        CommandResult.fail(e.message!!)
                    }
                    if (result.isSuccess()) {
                        when (command.cost) {
                            CommandCost.COOLDOWN -> {
                                user.cooldown.triggerCooldown(command.cooldownMillis)
                            }
                            CommandCost.CURRENCY -> {
                                val guild = Guild.getGuild(message.guild)
                                guild.addCurrency(user, Math.max(-command.price, -guild.getCurrency(user)))
                            }
                        }
                        if (command.votes && message.guild != null)
                            Guild.getGuild(message.guild).allowVotes(message)
                    } else {
                        Discord4J.LOGGER.info("Command failed with message: ${result.getFailMessage()}")
                        RequestBuffer.request {
                            MessageScheduler.sendTempMessage(Bot.data.DEFAULT_TEMP_MESSAGE_DURATION, message.channel, result.getFailMessage())
                        }
                        tryDelete(message)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                user.runningCommand = false
            }
        }
    }


    fun tryDelete(msg: IMessage) {
        RequestBuffer.request {
            try {
                msg.delete()
            } catch (e: MissingPermissionsException) {
            } catch (e: DiscordException) {
            }
        }
    }

}