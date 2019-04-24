package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import me.fzzy.robofzzy4j.listeners.VoiceListener
import me.fzzy.robofzzy4j.util.CommandResult
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.audio.AudioPlayer
import java.util.regex.Pattern

object Override : Command {

    override val cooldownCategory = "override"
    override val cooldownMillis = 0L
    override val description = "Only for bot owner, for modifying values in the bot"
    override val votes = false
    override val usageText = "override <command>"
    override val allowDM = true
    override val cost: Int = 1

    override fun runCommand(message: IMessage, args: List<String>): CommandResult {
        if (message.author.longID != Bot.client.applicationOwner.longID) return CommandResult.fail("sorry, but i only take override commands from ${Bot.client.applicationOwner.name}")

        when (args[0].toLowerCase()) {
            "volume" -> {
                AudioPlayer.getAudioPlayerForGuild(message.guild).volume = args[1].toFloat()
            }
            "currency" -> {
                Guild.getGuild(message.guild).addCurrency(message.author, 10)
                RequestBuffer.request { message.channel.sendMessage("${message.author.name} has ${Guild.getGuild(message.guild).getCurrency(message.author)} diamonds") }
            }
            "play" -> {
                val matcher = Pattern.compile("#(?<=v=)[a-zA-Z0-9-]+(?=&)|(?<=v/)[^&\\n]+|(?<=v=)[^&\\n]+|(?<=youtu.be/)[^&\\n]+#").matcher(args[1])

                val id = try {
                    if (matcher.find()) matcher.group(0) else args[1].split(".be/")[1]
                } catch (e: Exception) {
                    return CommandResult.fail("i couldnt get that videos id")
                }
                Play.play(message.guild.getVoiceChannelByID(args[2].toLong()), id)
            }
            "fullplay" -> {
                VoiceListener.overrides.add(args[1].toLong())
            }
            "skip" -> {
                AudioPlayer.getAudioPlayerForGuild(message.guild).skip()
            }
            "cooldowns", "cooldown" -> {
                val users = message.mentions
                for (user in users) {
                    for (cooldown in User.getUser((user.longID)).getAllCooldowns().values) {
                        cooldown.clearCooldown()
                    }
                }

                val userNames = arrayListOf<String>()
                for (i in 0 until users.size) {
                    userNames.add(if (i != users.size - 1) {
                        users[i].name.toLowerCase()
                    } else {
                        "and ${users[i].name.toLowerCase()}"
                    })
                }

                val messages = listOf(
                        "okay %author%! i reset %target%s cooldown${if (userNames.size > 1) "s" else ""}!",
                        "i guess ill do that if you want me to. i reset %target%s cooldown${if (userNames.size > 1) "s" else ""}",
                        "already done. %target%s cooldown${if (userNames.size > 1) "s are" else " is"} reset"
                )

                RequestBuffer.request {
                    MessageScheduler.sendTempMessage(Bot.data.DEFAULT_TEMP_MESSAGE_DURATION, message.channel, messages[Bot.random.nextInt(messages.size)]
                            .replace("%target%", userNames.joinToString(", "))
                            .replace("%author%", message.author.name.toLowerCase()))
                }
            }
            "allowvotes" -> {
                for (msg in message.channel.getMessageHistory(15)) {
                    if (msg.longID == args[1].toLong()) {
                        Guild.getGuild(msg.guild).allowVotes(msg)
                        break
                    }
                }
            }
        }

        return CommandResult.success()
    }

}