package me.fzzy.eventvoter.commands

import me.fzzy.eventvoter.Command
import me.fzzy.eventvoter.TempMessage
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.RequestBuffer

class Pfp : Command {

    override val attemptDelete: Boolean = true
    override val cooldownMillis: Long = 4 * 1000
    override val description: String = "Displays a users profile picture"
    override val usageText: String = "-pfp <user>"
    override val allowDM: Boolean = false

    override fun runCommand(event: MessageReceivedEvent, args: List<String>) {
        if (args.size == 1) {
            var finalUser: IUser? = null
            for (user in event.guild.users) {
                if (args[0].toLowerCase() == user.getDisplayName(event.guild).toLowerCase()){
                    finalUser = user
                    break
                }
            }
            if (finalUser == null) {
                for (user in event.guild.users) {
                    if (args[0].toLowerCase() == user.name.toLowerCase()){
                        finalUser = user
                        break
                    }
                }
            }
            if (finalUser == null) {
                RequestBuffer.request {
                    TempMessage(7 * 1000, event.channel.sendMessage("User not found!")).start()
                }
            } else {
                RequestBuffer.request { event.channel.sendMessage(finalUser.avatarURL) }
            }
        }
    }

}