package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import me.fzzy.robofzzy4j.listeners.VoiceListener
import me.fzzy.robofzzy4j.thread.IndividualTask
import me.fzzy.robofzzy4j.thread.Task
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
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

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        if (event.author.longID != Bot.client.applicationOwner.longID) return CommandResult.fail("sorry, but i only take override commands from ${Bot.client.applicationOwner.name}")

        when (args[0].toLowerCase()) {
            "volume" -> {
                AudioPlayer.getAudioPlayerForGuild(event.guild).volume = args[1].toFloat()
            }
            "play" -> {
                val matcher = Pattern.compile("#(?<=v=)[a-zA-Z0-9-]+(?=&)|(?<=v\\/)[^&\\n]+|(?<=v=)[^&\\n]+|(?<=youtu.be/)[^&\\n]+#").matcher(args[0])

                val id = try {
                    if (matcher.find()) matcher.group(0) else args[2].split(".be/")[1]
                } catch (e: Exception) {
                    return CommandResult.fail("i couldnt get that videos id")
                }
                Play.play(event.guild.getVoiceChannelByID(args[1].toLong()), id)
            }
            "fullplay" -> {
                VoiceListener.overrides.add(args[1].toLong())
            }
            "skip" -> {
                AudioPlayer.getAudioPlayerForGuild(event.guild).skip()
            }
            "cooldowns", "cooldown" -> {
                val users = event.message.mentions
                Task.registerTask(IndividualTask({
                    for (user in users) {
                        for (cooldown in User.getUser((user.longID)).getAllCooldowns().values) {
                            cooldown.clearCooldown()
                        }
                    }
                }, 1, false))

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
                    MessageScheduler.sendTempMessage(10000, event.channel, messages[Bot.random.nextInt(messages.size)]
                            .replace("%target%", userNames.joinToString(", "))
                            .replace("%author%", event.author.name.toLowerCase()))
                }
            }
            "allowvotes" -> {
                for (message in event.channel.getMessageHistory(15)) {
                    if (message.longID == args[1].toLong()) {
                        Guild.getGuild(event.guild).allowVotes(message)
                        break
                    }
                }
            }
        }

        return CommandResult.success()
    }

}