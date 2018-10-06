package me.fzzy.robofzzy4j

import sx.blah.discord.Discord4J
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

private lateinit var commandMap: HashMap<String, Command>

lateinit var commandPrefix: String

class CommandHandler constructor(prefix: String) {

    init {
        commandPrefix = prefix
        commandMap = hashMapOf()
    }

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
        if (!args[0].startsWith(commandPrefix))
            return
        val commandString = args[0].substring(1)
        var argsList: List<String> = args.toMutableList()
        argsList = argsList.drop(1)

        val user = User.getUser(event.author)

        if (commandMap.containsKey(commandString.toLowerCase())) {

            val command = commandMap[commandString.toLowerCase()]!!

            if (event.channel.isPrivate) {
                if (!command.allowDM) {
                    RequestBuffer.request { messageScheduler.sendTempMessage(DEFAULT_TEMP_MESSAGE_DURATION, event.channel, "This command is not allowed in DMs!") }
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
                    if (!command.votes)
                        tryDelete(event.message)
                    Thread {
                        try {
                            val result = command.runCommand(event, argsList)
                            if (result.isSuccess()) {
                                user.cooldowns.triggerCooldown(commandString)
                                if (command.votes)
                                    guild.allowVotes(event.message)
                            } else {
                                Discord4J.LOGGER.info("Command failed with message: ${result.getFailMessage()}")
                                RequestBuffer.request {
                                    messageScheduler.sendTempMessage(10 * 1000, event.channel, result.getFailMessage())
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        user.runningCommand = false
                    }.start()
                }
            } else {
                tryDelete(event.message)
                val timeLeft = (trueCooldown - timePassedCommand) / 1000
                val message = "${event.author.getDisplayName(event.guild)}! You are on cooldown for $timeLeft more seconds."
                RequestBuffer.request {
                    val msg = Funcs.sendMessage(event.channel, message)
                    if (msg != null)
                        CooldownMessage(timeLeft.toInt(), event.author.getDisplayName(event.guild), msg).start()
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

class CooldownMessage constructor(private var cooldown: Int, private var userName: String, private var msg: IMessage) : Thread() {
    override fun run() {
        while (cooldown > 0) {
            Thread.sleep(1000L)
            cooldown--
        }
        if (msg.channel.getMessageHistory(10).contains(msg)) {
            RequestBuffer.request { msg.edit("$userName, you are no longer on cooldown.") }
            Thread.sleep(20000L)
        }
        RequestBuffer.request { msg.delete() }
    }
}