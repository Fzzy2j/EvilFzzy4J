package me.fzzy.eventvoter

import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.util.*

private lateinit var commandMap: HashMap<String, Command>
private lateinit var cooldowns: HashMap<Long, Cooldown>

lateinit var commandPrefix: String

const val generalCommandCooldownMillis: Long = 1 * 1000

class CommandHandler constructor(prefix: String) {

    init {
        commandPrefix = prefix
        commandMap = hashMapOf()
        cooldowns = hashMapOf()
    }

    fun registerCommand(string: String, command: Command): Boolean {
        if (string.toLowerCase() == "generalcommands")
            return false
        commandMap[string.toLowerCase()] = command
        return true
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
        } else {
            return false
        }
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

        if (commandMap.containsKey(commandString.toLowerCase())) {
            if (!cooldowns.containsKey(event.author.longID))
                cooldowns[event.author.longID] = Cooldown()

            val command = commandMap[commandString.toLowerCase()]!!

            if (command.attemptDelete) {
                RequestBuffer.request {
                    try {
                        event.message.delete()
                    } catch (e: MissingPermissionsException) {
                    } catch (e: DiscordException) {
                    }
                }
            }

            if (event.channel.isPrivate) {
                if (!command.allowDM) {
                    RequestBuffer.request { TempMessage(7 * 1000, event.channel.sendMessage("This command is not allowed in DMs!")).start() }
                    return
                }
            }

            val commandCooldown = if (command.cooldownMillis > generalCommandCooldownMillis) command.cooldownMillis else generalCommandCooldownMillis
            val timePassedCommand = cooldowns[event.author.longID]!!.getTimePassedMillis(commandString)
            val timePassedGeneral = cooldowns[event.author.longID]!!.getTimePassedMillis("generalCommands")
            if (timePassedCommand > commandCooldown && timePassedGeneral > generalCommandCooldownMillis) {
                cooldowns[event.author.longID]?.triggerCooldown(commandString)
                cooldowns[event.author.longID]?.triggerCooldown("generalCommands")

                println("${event.author.name} running command: ${commandString.toLowerCase()}")
                command.runCommand(event, argsList)
            } else {
                val timeLeft = (commandCooldown - timePassedCommand) / 1000
                val message = "${event.author.getDisplayName(event.guild)}! You are on cooldown for $timeLeft seconds."
                RequestBuffer.request { CooldownMessage(timeLeft.toInt(), event.channel, event.author.getDisplayName(event.guild), event.channel.sendMessage(message)).start() }
            }
        }
    }

}

class CooldownMessage constructor(private var cooldown: Int, private var channel: IChannel, private var userName: String, private var msg: IMessage) : Thread() {
    override fun run() {
        while (cooldown > 0) {
            if (cooldown % 5 == 0) {
                RequestBuffer.request { msg.edit("$userName! You are on cooldown for $cooldown seconds.") }
            }
            Thread.sleep(1000L)
            cooldown--
        }
        RequestBuffer.request { msg.edit("$userName, you are no longer on cooldown.") }
        Thread.sleep(7000L)
        RequestBuffer.request { msg.delete() }
    }
}