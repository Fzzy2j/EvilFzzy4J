package me.fzzy.evilfzzy4j.command.admin

import discord4j.core.`object`.util.Snowflake
import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import me.fzzy.evilfzzy4j.FzzyUser
import me.fzzy.evilfzzy4j.MessageScheduler
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.CommandCost
import me.fzzy.evilfzzy4j.command.CommandResult
import reactor.core.publisher.Mono

object Override : Command("override") {

    override val cooldownMillis = 0L
    override val description = "Only for bot owner, for modifying values in the bot"
    override val votes = false
    override val args: ArrayList<String> = arrayListOf("command")
    override val allowDM = true
    override val price: Int = 0
    override val cost: CommandCost = CommandCost.CURRENCY

    override fun runCommand(message: CachedMessage, args: List<String>): Mono<CommandResult> {
        val owner = Bot.client.applicationInfo.block()!!.owner.block()!!
        val failText = "sorry, but i only take override command from ${owner.username} ${Bot.sadEmoji()}"
        if (message.author.id != owner.id) return Mono.just(CommandResult.fail(failText))

        when (args[0].toLowerCase()) {
            "volume" -> {
                FzzyGuild.getGuild(message.guild.id).player.provider.player.volume = args[1].toInt()
                //AudioPlayer.getAudioPlayerForGuild(message.guild).volume = args[1].toFloat()
            }
            "test" -> {
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
                for (mention in message.userMentions!!) {
                    FzzyGuild.getGuild(message.guild.id).addCurrency(mention, args[1].toInt())
                }
                MessageScheduler.sendTempMessage(message.channel, "done!", Bot.data.DEFAULT_TEMP_MESSAGE_DURATION).block()
            }
            "cooldowns", "cooldown" -> {
                val users = message.userMentions ?: return Mono.just(CommandResult.fail("theres no mentions in that ${Bot.sadEmoji()}"))
                for (user in users) {
                    FzzyUser.getUser(user.id).cooldown.clearCooldown()
                }

                val userNames = arrayListOf<String>()
                for (i in 0 until users.size) {
                    userNames.add(if (i != users.size - 1) {
                        users[i].username.toLowerCase()
                    } else {
                        "and ${users[i].username.toLowerCase()}"
                    })
                }

                val messages = listOf(
                        "okay %author%! i reset %target%s cooldown${if (userNames.size > 1) "s" else ""} ${Bot.happyEmoji()}",
                        "i guess ill do that if you want me to. i reset %target%s cooldown${if (userNames.size > 1) "s" else ""} ${Bot.happyEmoji()}",
                        "already done. %target%s cooldown${if (userNames.size > 1) "s are" else " is"} reset ${Bot.happyEmoji()}"
                )

                MessageScheduler.sendTempMessage(message.channel, messages[Bot.random.nextInt(messages.size)]
                        .replace("%target%", userNames.joinToString(", "))
                        .replace("%author%", message.author.username.toLowerCase()), Bot.data.DEFAULT_TEMP_MESSAGE_DURATION).block()
            }
            "allowvotes" -> {
                val msg = message.channel.getMessageById(Snowflake.of(args[1].toLong())).block()
                if (msg != null)
                    FzzyGuild.getGuild(message.guild.id).allowVotes(msg)
            }
        }

        return Mono.just(CommandResult.success())
    }

}