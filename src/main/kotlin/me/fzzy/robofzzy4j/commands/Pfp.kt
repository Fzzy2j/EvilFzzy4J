package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.RequestBuffer

class Pfp : Command {

    override val cooldownMillis: Long = 4 * 1000
    override val votes: Boolean = false
    override val description: String = "Displays a users profile picture"
    override val usageText: String = "-pfp <user>"
    override val allowDM: Boolean = false

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {

        if (args.isEmpty())
            return CommandResult.fail("Invalid syntax. $usageText")

        var toCheck = ""
        for (text in args) {
            toCheck += " $text"
        }
        toCheck = toCheck.substring(1)
        var finalUser: IUser? = null
        for (user in event.guild.users) {
            if (toCheck.toLowerCase() == user.getDisplayName(event.guild).toLowerCase()) {
                finalUser = user
                break
            }
        }
        if (finalUser == null) {
            for (user in event.guild.users) {
                if (toCheck.toLowerCase() == user.name.toLowerCase()) {
                    finalUser = user
                    break
                }
            }
        }
        if (finalUser == null)
            return CommandResult.fail("User not found!")

        RequestBuffer.request { Funcs.sendMessage(event.channel, finalUser.avatarURL) }

        return CommandResult.success()
    }

}