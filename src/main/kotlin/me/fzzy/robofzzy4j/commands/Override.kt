package me.fzzy.robofzzy4j.commands

import me.fzzy.robofzzy4j.*
import me.fzzy.robofzzy4j.util.CommandCost
import me.fzzy.robofzzy4j.util.CommandResult
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.audio.AudioPlayer

object Override : Command("override") {

    override val cooldownMillis = 0L
    override val description = "Only for bot owner, for modifying values in the bot"
    override val votes = false
    override val args: ArrayList<String> = arrayListOf("command")
    override val allowDM = true
    override val price: Int = 0
    override val cost: CommandCost = CommandCost.CURRENCY

    override fun runCommand(message: IMessage, args: List<String>): CommandResult {
        val failText = "sorry, but i only take override commands from ${Bot.client.applicationOwner.name} ${Bot.COMPLACENT_EMOJI}"
        if (message.author.longID != Bot.client.applicationOwner.longID) return CommandResult.fail(failText)

        when (args[0].toLowerCase()) {
            "volume" -> {
                AudioPlayer.getAudioPlayerForGuild(message.guild).volume = args[1].toFloat()
            }
            "play" -> {
                Play.play(message.guild.getVoiceChannelByID(args[2].toLong()), args[1])
            }
            "fullplay" -> {
                AudioPlayer.getAudioPlayerForGuild(message.guild).currentTrack.metadata["fzzyTimeSeconds"] = 60 * 60 * 24
            }
            "skip" -> {
                AudioPlayer.getAudioPlayerForGuild(message.guild).skip()
            }
            "give" -> {
                for (mention in message.mentions) {
                    Guild.getGuild(message.guild).addCurrency(mention, args[1].toInt())
                }
                RequestBuffer.request { MessageScheduler.sendTempMessage(Bot.data.DEFAULT_TEMP_MESSAGE_DURATION, message.channel, "done!") }
            }
            "cooldowns", "cooldown" -> {
                val users = message.mentions
                for (user in users) {
                    User.getUser(user.longID).cooldown.clearCooldown()
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
                        "okay %author%! i reset %target%s cooldown${if (userNames.size > 1) "s" else ""} ${Bot.COMPLACENT_EMOJI}",
                        "i guess ill do that if you want me to. i reset %target%s cooldown${if (userNames.size > 1) "s" else ""} ${Bot.HAPPY_EMOJI}",
                        "already done. %target%s cooldown${if (userNames.size > 1) "s are" else " is"} reset ${Bot.COMPLACENT_EMOJI}"
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