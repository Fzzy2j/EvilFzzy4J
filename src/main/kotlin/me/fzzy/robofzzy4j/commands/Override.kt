package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import me.fzzy.robofzzy4j.listeners.VoiceListener
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.audio.AudioPlayer

object Override : Command {
    override val cooldownMillis = 2000L
    override val description = "Only for bot owner, for modifying values in the bot"
    override val votes = false
    override val usageText = "-override <command>"
    override val allowDM = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        if (event.author.longID != cli.applicationOwner.longID) return CommandResult.fail("This command is only for the bot owner!")

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
                User.getUser(args[1].toLong()).cooldowns.clearCooldowns()
            }
            "play" -> {
                for (voice in event.guild.voiceChannels) {
                    if (voice.name.toLowerCase().startsWith(args[1].toLowerCase())) {
                        VoiceListener.overrides.add(event.messageID)
                        return Play.play(voice, args[2], event.messageID)
                    }
                }
            }
            "allowvotes" -> {
                Guild.getGuild(event.guild).allowVotes(event.guild.getMessageByID(args[1].toLong()))
            }
        }

        return CommandResult.success()
    }

}