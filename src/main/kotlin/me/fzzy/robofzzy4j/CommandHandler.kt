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
        if (!args[0].startsWith(RoboFzzy.BOT_PREFIX))
            return
        val commandString = args[0].substring(1)
        var argsList: List<String> = args.toMutableList()
        argsList = argsList.drop(1)

        val user = User.getUser(event.author)

        if (commandMap.containsKey(commandString.toLowerCase())) {

            val command = commandMap[commandString.toLowerCase()]!!

            if (event.channel.isPrivate) {
                if (!command.allowDM) {
                    RequestBuffer.request { MessageScheduler.sendTempMessage(RoboFzzy.DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "This command is not allowed in DMs!") }
                    return
                }
            }

            val date = SimpleDateFormat("hh:mm:ss aa").format(Date(System.currentTimeMillis()))
            Discord4J.LOGGER.info("$date - ${event.author.name}#${event.author.discriminator} running command: ${event.message.content}")

            if (user.cooldown.isReady((100 - user.getCooldownModifier(Guild.getGuild(event.guild))) / 100.0)) {
                if (!user.runningCommand) {
                    user.runningCommand = true
                    if (!command.votes) tryDelete(event.message)
                    RoboFzzy.executor.submit {
                        try {
                            val result = try {
                                command.runCommand(event, argsList)
                            } catch (e: java.lang.Exception) {
                                CommandResult.fail("Command failed $e")
                            }
                            if (result.isSuccess()) {
                                user.cooldown.triggerCooldown(command.cooldownMillis)
                                if (command.votes && event.guild != null)
                                    Guild.getGuild(event.guild).allowVotes(event.message)
                            } else {
                                Discord4J.LOGGER.info("Command failed with message: ${result.getFailMessage()}")
                                RequestBuffer.request {
                                    MessageScheduler.sendTempMessage(10 * 1000, event.channel, result.getFailMessage())
                                }
                                tryDelete(event.message)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        user.runningCommand = false
                    }
                }
            } else {
                tryDelete(event.message)
                val timeLeft = Math.ceil((user.cooldown.timeLeft((100 - user.getCooldownModifier(Guild.getGuild(event.guild))) / 100.0)) / 1000.0 / 60.0).roundToInt()

                val messages = arrayOf(
                        "%user% you can use that command in %time%",
                        "you can use that command in %time% %user%",
                        "sorry %user%, i cant let you use that command for another %time%"
                )
                RequestBuffer.request {
                    MessageScheduler.sendTempMessage(10000, event.channel, messages[RoboFzzy.random.nextInt(messages.size)]
                            .replace("%user%", event.author.name.toLowerCase())
                            .replace("%time%", timeLeft.toString() + if (timeLeft == 1) " minute" else " minutes")
                    )
                }
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