package me.fzzy.robofzzy4j

import me.fzzy.robofzzy4j.util.CommandResult
import sx.blah.discord.Discord4J
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.RequestBuilder
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

            val date = SimpleDateFormat("hh:mm:ss aa").format(Date(System.currentTimeMillis()))
            Discord4J.LOGGER.info("$date - ${event.author.name}#${event.author.discriminator} running command: ${event.message.content}")
            if (user.id == Bot.client.applicationOwner.longID ||
                    user.getCooldown(command.cooldownCategory).isReady((100 - user.getCooldownModifier(Guild.getGuild(event.guild))) / 100.0)) {
                runCommand(user, command, event.message, argsList)
            } else {
                val timeLeftMinutes = Math.ceil((user.getCooldown(command.cooldownCategory).timeLeft((100 - user.getCooldownModifier(Guild.getGuild(event.guild))) / 100.0)) / 1000.0 / 60.0).roundToInt()

                val s = if (command.cost == 1) "" else "s"
                var content = "${event.author.name.toLowerCase()} you are still on cooldown for $timeLeftMinutes minute${if (timeLeftMinutes != 1) "s" else ""}"

                var msg: IMessage? = null
                val msgBuilder = RequestBuilder(Bot.client).doAction {
                    msg = MessageScheduler.sendTempMessage(Bot.data.DEFAULT_TEMP_MESSAGE_DURATION, event.channel, content)
                    true
                }

                if (!Bot.data.cooldownMode) {
                    content += "\n click the reaction to spend ${command.cost} point$s to skip the cooldown"
                    msgBuilder.shouldBufferRequests(true).andThen {
                        try {
                            msg?.addReaction(Bot.CURRENCY_EMOJI)
                        } catch (e: MissingPermissionsException) {
                        }
                        true
                    }
                } else tryDelete(event.message)

                msgBuilder.execute()
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
                        user.getCooldown(command.cooldownCategory).triggerCooldown(command.cooldownMillis)
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