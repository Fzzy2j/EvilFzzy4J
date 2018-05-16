package me.fzzy.eventvoter

import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionRemoveEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.RequestBuilder
import java.util.regex.Matcher
import java.util.regex.Pattern
import sx.blah.discord.util.audio.AudioPlayer
import java.io.File


class Events {

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

            // Commands for memes server
            if (event.message.content.startsWith("-") && event.message.content.length > 1) {
                if (event.guild.longID == 214250278466224128) {
                    if (event.message.content.equals("-all", true)) {
                        var all = ""
                        for (file in File("sounds").listFiles()) {
                            all += "-${file.nameWithoutExtension}\n"
                        }
                        event.message.author.orCreatePMChannel.sendMessage(all)
                        return
                    }
                    val userVoiceChannel = event.author.getVoiceStateForGuild(event.guild).channel ?: return

                    val audioP = AudioPlayer.getAudioPlayerForGuild(event.guild)

                    val audioDir = File("sounds").listFiles { file -> file.name.contains(event.message.content.substring(1)) }

                    if (audioDir == null || audioDir.isEmpty())
                        return

                    Sound(userVoiceChannel, audioP, audioDir[0], event.guild).start()
                }
            }
        }
    }

    @EventSubscriber
    fun onReactionAdd(event: ReactionAddEvent) {
        val leaderboard = getLeaderboard(event.guild.longID)
        if (leaderboard != null) {
            if (event.reaction.getUserReacted(cli.ourUser)) {
                if (event.message.author.longID != event.user.longID) {
                    when (event.reaction.emoji.name) {
                        "upvote" -> leaderboard.addToScore(event.author.longID, 1)
                        "downvote" -> leaderboard.addToScore(event.author.longID, -1)
                    }
                    leaderboard.updateLeaderboard()
                }
            }
        }
    }

    @EventSubscriber
    fun onReactionRemove(event: ReactionRemoveEvent) {
        val leaderboard = getLeaderboard(event.guild.longID)
        if (leaderboard != null) {
            if (event.reaction.getUserReacted(cli.ourUser)) {
                if (event.message.author.longID != event.user.longID) {
                    when (event.reaction.emoji.name) {
                        "upvote" -> leaderboard.addToScore(event.author.longID, -1)
                        "downvote" -> leaderboard.addToScore(event.author.longID, 1)
                    }
                    leaderboard.updateLeaderboard()
                }
            }
        }
    }

    @EventSubscriber
    fun onReady(event: ReadyEvent) {
        RequestBuffer.request { cli.changePresence(StatusType.ONLINE, ActivityType.LISTENING, "the rain") }
        for (guild in cli.guilds) {
            val leaderboard = Leaderboard(guild.longID)
            guilds.add(leaderboard)
            leaderboard.loadLeaderboard()
            leaderboard.updateLeaderboard()
        }
    }
}
