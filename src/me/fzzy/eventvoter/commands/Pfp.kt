package me.fzzy.eventvoter.commands

import me.fzzy.eventvoter.Command
import me.fzzy.eventvoter.TempMessage
import me.fzzy.eventvoter.sendMessage
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
        if (args.isNotEmpty()) {
            var toCheck: String = ""
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
            if (finalUser == null) {
                RequestBuffer.request {
                    val msg = sendMessage(event.channel, "User not found!")
                    if (msg != null)
                        TempMessage(7 * 1000, msg).start()
                }
            } else {
                RequestBuffer.request { sendMessage(event.channel, finalUser.avatarURL) }
            }
        }
    }

}