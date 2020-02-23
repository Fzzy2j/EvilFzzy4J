package me.fzzy.evilfzzy4j.command.admin

import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import me.fzzy.evilfzzy4j.FzzyUser
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandResult
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import java.util.concurrent.TimeUnit

object Override : Command("override") {

    override val cooldownMillis = 0L
    override val description = "Only for bot owner, for modifying values in the bot"
    override val args: ArrayList<String> = arrayListOf("command")
    override val allowDM = true

    override fun runCommand(event: MessageReceivedEvent, args: List<String>, latestMessageId: Long): CommandResult {
        val owner = Bot.client.retrieveApplicationInfo().complete().owner
        val failText = "sorry, but i only take override command from ${owner.name} ${Bot.sadEmoji.asMention}"
        if (event.message.author.id != owner.id) return CommandResult.fail(failText)

        when (args[0].toLowerCase()) {
            "volume" -> {
                //FzzyGuild.getGuild(message.guild.id).player.provider.player.volume = args[1].toInt()
                //AudioPlayer.getAudioPlayerForGuild(message.guild).volume = args[1].toFloat()
            }
            "play" -> {
                //Play.play(message.guild.getVoiceChannelByID(args[2].toLong()), args[1])
            }
            "fullplay" -> {
                //AudioPlayer.getAudioPlayerForGuild(message.guild).currentTrack.metadata["fzzyTimeSeconds"] = 60 * 60 * 24
            }
            "skip" -> {
                //AudioPlayer.getAudioPlayerForGuild(message.guild).skip()
            }
            "give" -> {
                for (mention in event.message.mentionedUsers) {
                    FzzyGuild.getGuild(event.message.guild.id).addCurrency(mention, args[1].toInt())
                }
                event.channel.sendMessage("done!").queue { msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES) }
            }
            "cooldowns", "cooldown" -> {
                val users = event.message.mentionedUsers
                for (user in users) {
                    FzzyUser.getUser(user.idLong).cooldown.clearCooldown()
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
                        "okay %author%! i reset %target%s cooldown${if (userNames.size > 1) "s" else ""} ${Bot.happyEmoji.asMention}",
                        "i guess ill do that if you want me to. i reset %target%s cooldown${if (userNames.size > 1) "s" else ""} ${Bot.happyEmoji.asMention}",
                        "already done. %target%s cooldown${if (userNames.size > 1) "s are" else " is"} reset ${Bot.happyEmoji.asMention}"
                )

                val text = messages[Bot.random.nextInt(messages.size)]
                        .replace("%target%", userNames.joinToString(", "))
                        .replace("%author%", event.message.author.name.toLowerCase())

                event.channel.sendMessage(text).queue { msg -> msg.delete().queueAfter(1, TimeUnit.MINUTES) }
            }
            "allowvotes" -> {
                event.channel.retrieveMessageById(args[1].toLong()).queue { msg ->
                    run {
                        FzzyGuild.getGuild(event.message.guild.id).allowVotes(msg)
                    }
                }
            }
        }

        return CommandResult.success()
    }

}