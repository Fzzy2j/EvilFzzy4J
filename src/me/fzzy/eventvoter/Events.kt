package me.fzzy.eventvoter

import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionRemoveEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.RequestBuilder
import java.util.regex.Matcher
import java.util.regex.Pattern
import sx.blah.discord.util.audio.AudioPlayer
import java.io.File

const val FZZY_ID = 66104132028604416L
const val MEMES_ID = 214250278466224128L

class Events {

    private var cooldowns: HashMap<Long, Long> = hashMapOf()

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.message.author.isBot) {
            if (getLeaderboard(event.guild.longID) == null)
                guilds.add(Leaderboard(event.guild.longID))
            val m: Matcher = Pattern.compile("^((https?|ftp)://|(www|ftp)\\.)?[a-z0-9-]+(\\.[a-z0-9-]+)+([/?].*)?$").matcher(event.message.content)
            if (m.find() || event.message.attachments.size > 0) {
                RequestBuilder(event.client).shouldBufferRequests(true).doAction {
                    event.message.addReaction(ReactionEmoji.of("upvote", 445376322353496064L))
                    true
                }.andThen {
                    event.message.addReaction(ReactionEmoji.of("downvote", 445376330989830147L))
                    true
                }.execute()
            }

            // Commands for memes server and stop command
            val soundCooldown = 30
            if (event.message.content.startsWith("-") && event.message.content.length > 1) {

                // Stop the bot
                if (event.message.content.equals("-stop", true)) {
                    if (event.author.longID == FZZY_ID) {
                        for (leaderboard in guilds) {
                            leaderboard.saveLeaderboard()
                        }
                        cli.logout()
                        running = false
                        System.exit(0)
                    }
                }
                if (event.guild.longID == MEMES_ID) {
                    if (event.message.content.equals("-all", true)) {
                        var all = ""
                        for (file in File("sounds").listFiles()) {
                            all += "-${file.nameWithoutExtension}\n"
                        }
                        event.message.author.orCreatePMChannel.sendMessage(all)
                        return
                    }
                    if (cli.ourUser.getVoiceStateForGuild(event.guild).channel == null) {
                        event.message.delete()
                        if (System.currentTimeMillis() - cooldowns.getOrDefault(event.author.longID, 0) > soundCooldown * 1000) {
                            val userVoiceChannel = event.author.getVoiceStateForGuild(event.guild).channel ?: return
                            val audioP = AudioPlayer.getAudioPlayerForGuild(event.guild)
                            val audioDir = File("sounds").listFiles { file -> file.name.contains(event.message.content.substring(1)) }

                            if (audioDir == null || audioDir.isEmpty())
                                return

                            Sound(userVoiceChannel, audioP, audioDir[0], event.guild).start()
                            cooldowns[event.author.longID] = System.currentTimeMillis()
                        } else {
                            val timeLeft = soundCooldown - ((System.currentTimeMillis() - cooldowns.getOrDefault(event.author.longID, System.currentTimeMillis())) / 1000)
                            val message = "${event.author.getDisplayName(event.guild)}! you are still on cooldown for $timeLeft second${if (timeLeft == 1L) "" else "s"}"
                            TempMessage(5000, event.channel, message).start()
                        }
                    }
                }
            }
        }
    }

    @EventSubscriber
    fun onReactionAdd(event: ReactionAddEvent) {
        val leaderboard = getLeaderboard(event.guild.longID)
        if (leaderboard != null) {
            if (System.currentTimeMillis() / 1000 - event.message.timestamp.epochSecond < 60 * 60 * 24) {
                if (event.reaction.getUserReacted(cli.ourUser)) {
                    if (event.message.author.longID != event.user.longID) {
                        when (event.reaction.emoji.name) {
                            "upvote" -> leaderboard.addToScore(event.author.longID, 1)
                            "downvote" -> leaderboard.addToScore(event.author.longID, -1)
                        }
                    }
                }
            }
        }
    }

    @EventSubscriber
    fun onReactionRemove(event: ReactionRemoveEvent) {
        val leaderboard = getLeaderboard(event.guild.longID)
        if (leaderboard != null) {
            if (System.currentTimeMillis() / 1000 - event.message.timestamp.epochSecond < 60 * 60 * 24) {
                if (event.reaction.getUserReacted(cli.ourUser)) {
                    if (event.message.author.longID != event.user.longID) {
                        when (event.reaction.emoji.name) {
                            "upvote" -> leaderboard.addToScore(event.author.longID, -1)
                            "downvote" -> leaderboard.addToScore(event.author.longID, 1)
                        }
                    }
                }
            }
        }
    }

    @EventSubscriber
    fun onReady(event: ReadyEvent) {
        for (guild in cli.guilds) {
            val leaderboard = Leaderboard(guild.longID)
            guilds.add(leaderboard)
            leaderboard.loadLeaderboard()
            leaderboard.updateLeaderboard()
        }
        RequestBuffer.request { cli.changePresence(StatusType.ONLINE, ActivityType.LISTENING, "the rain") }
    }
}

class TempMessage constructor(private var delay: Long, private var channel: IChannel, private var message: String): Thread() {
    override fun run() {
        var mes: IMessage? = null
        RequestBuffer.request { mes = channel.sendMessage(message) }
        Thread.sleep(delay)
        mes?.delete()
    }
}
