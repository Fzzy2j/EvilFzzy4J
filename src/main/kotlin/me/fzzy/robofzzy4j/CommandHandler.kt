package me.fzzy.robofzzy4j

import sx.blah.discord.Discord4J
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.text.SimpleDateFormat
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

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {
        val args = event.message.content.split(" ")
        if (args.isEmpty())
            return
        if (!args[0].startsWith(BOT_PREFIX))
            return
        val commandString = args[0].substring(1)
        var argsList: List<String> = args.toMutableList()
        argsList = argsList.drop(1)

        val user = User.getUser(event.author)

        if (commandMap.containsKey(commandString.toLowerCase())) {

            val command = commandMap[commandString.toLowerCase()]!!

            if (event.channel.isPrivate) {
                if (!command.allowDM) {
                    RequestBuffer.request { MessageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "This command is not allowed in DMs!") }
                    return
                }
            }

            val date = SimpleDateFormat("hh:mm:ss aa").format(Date(System.currentTimeMillis()))
            Discord4J.LOGGER.info("$date - ${event.author.name}#${event.author.discriminator} running command: ${event.message.content}")

            val guild = Guild.getGuild(event.guild)
            val timePassedCommand = user.cooldowns.getTimePassedMillis(commandString)
            val trueCooldown = command.cooldownMillis * ((100 - user.getCooldownModifier(guild)) / 100.0).roundToInt()

            if (timePassedCommand > trueCooldown) {

                if (!user.runningCommand) {
                    user.runningCommand = true
                    Thread {
                        try {
                            val result = try {
                                command.runCommand(event, argsList)
                            } catch (e: java.lang.Exception) {
                                CommandResult.fail("Command failed $e")
                            }
                            if (result.isSuccess()) {
                                user.cooldowns.triggerCooldown(commandString)
                                if (command.votes)
                                    guild.allowVotes(event.message)
                                else
                                    notifyProcessSuccess(event.message)
                            } else {
                                Discord4J.LOGGER.info("Command failed with message: ${result.getFailMessage()}")
                                RequestBuffer.request {
                                    MessageScheduler.sendTempMessage(10 * 1000, event.channel, result.getFailMessage())
                                }
                                notifyProcessFail(event.message)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        user.runningCommand = false
                    }.start()
                }
            } else {
                notifyProcessFail(event.message)
                val timeLeft = (trueCooldown - timePassedCommand)
                val endDate = Date(System.currentTimeMillis() + timeLeft)
                val format = SimpleDateFormat("hh:mm.ssaa").format(endDate).toLowerCase()

                val messages = arrayOf(
                        "%user% youll be off cooldown at %time%",
                        "your cooldown will be over at %time% %user%",
                        "you gotta slow down %user%, your cooldown will end at %time%"
                )
                RequestBuffer.request {
                    Funcs.sendMessage(event.channel, messages[random.nextInt(messages.size)]
                            .replace("%user%", event.author.getDisplayName(event.guild).toLowerCase())
                            .replace("%time%", if (format.startsWith("0")) format.substring(1) else format)
                    )
                }
            }
        }
    }

    fun notifyProcessSuccess(msg: IMessage) {
        RequestBuffer.request {
            try {
                msg.addReaction(GREENTICK_EMOJI)
            } catch (e: MissingPermissionsException) {
            } catch (e: DiscordException) {
            }
        }
    }

    fun notifyProcessFail(msg: IMessage) {
        RequestBuffer.request {
            try {
                msg.addReaction(REDTICK_EMOJI)
            } catch (e: MissingPermissionsException) {
            } catch (e: DiscordException) {
            }
        }
    }
}

class CommandResult private constructor(private val success: Boolean, private val message: String) {

    companion object {
        fun success(): CommandResult {
            return CommandResult(true, "")
        }

        fun fail(msg: String): CommandResult {
            return CommandResult(false, msg)
        }
    }

    fun isSuccess(): Boolean {
        return success
    }

    fun getFailMessage(): String {
        return message
    }

}