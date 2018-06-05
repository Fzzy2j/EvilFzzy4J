package me.fzzy.eventvoter

import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import java.util.*

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

    fun unregisterCommand(string: String) {
        if (commandMap.containsKey(string))
            commandMap.remove(string)
    }

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {
        val args = event.message.content.split(" ")
        if (args.isEmpty())
            return
        if (!args[0].startsWith(commandPrefix))
            return
        val command = args[0].substring(1)
        val argsList: MutableList<String> = args.toMutableList()
        argsList.removeAt(0)

        if (commandMap.containsKey(command.toLowerCase()))
            commandMap[command]?.runCommand(event, args)
    }

}