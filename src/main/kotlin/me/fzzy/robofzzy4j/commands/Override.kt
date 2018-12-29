package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import me.fzzy.robofzzy4j.listeners.VoiceListener
import me.fzzy.robofzzy4j.thread.IndividualTask
import me.fzzy.robofzzy4j.thread.Task
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.audio.AudioPlayer

object Override : Command {
    override val cooldownMillis = 0L
    override val description = "Only for bot owner, for modifying values in the bot"
    override val votes = false
    override val usageText = "-override <command>"
    override val allowDM = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        if (event.author.longID != RoboFzzy.cli.applicationOwner.longID) return CommandResult.fail("sorry, but i only take override commands from ${RoboFzzy.cli.applicationOwner.name}")

        when (args[0].toLowerCase()) {
            "volume" -> {
                AudioPlayer.getAudioPlayerForGuild(event.guild).volume = args[1].toFloat()
            }
            "fullplay" -> {
                VoiceListener.overrides.add(args[1].toLong())
            }
            "skip" -> {
                AudioPlayer.getAudioPlayerForGuild(event.guild).skip()
            }
            "cooldowns" -> {
                Task.registerTask(IndividualTask({
                    User.getUser(args[1].toLong()).cooldown.clearCooldown()
                }, 1, false))
                val messages = listOf(
                        "okay %author%! i reset %target%s cooldown!",
                        "i guess ill do that if you want me to. i reset %target%s cooldown",
                        "already done. %target%s cooldown is reset"
                )
                RequestBuffer.request {
                    event.channel.sendMessage(messages[RoboFzzy.random.nextInt(messages.size)]
                            .replace("%target%", RoboFzzy.cli.getUserByID(args[1].toLong()).name.toLowerCase())
                            .replace("%author%", event.author.name.toLowerCase())
                    )
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