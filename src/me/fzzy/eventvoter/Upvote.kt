package me.fzzy.eventvoter

import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionRemoveEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.RequestBuilder
import java.util.regex.Matcher
import java.util.regex.Pattern

class Upvote {

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.message.author.isBot) {
            if (event.guild != null) {
                if (getLeaderboard(event.guild.longID) == null)
                    guilds.add(Leaderboard(event.guild.longID))
                val pattern = Pattern.compile("((http:\\/\\/|https:\\/\\/)?(www.)?(([a-zA-Z0-9-]){2,}\\.){1,4}([a-zA-Z]){2,6}(\\/([a-zA-Z-_\\/\\.0-9#:?=&;,]*)?)?)")
                val m: Matcher = pattern.matcher(event.message.content)
                if (m.find() || event.message.attachments.size > 0) {
                    RequestBuilder(event.client).shouldBufferRequests(true).doAction {
                        event.message.addReaction(ReactionEmoji.of("upvote", 445376322353496064L))
                        true
                    }.andThen {
                        event.message.addReaction(ReactionEmoji.of("downvote", 445376330989830147L))
                        true
                    }.execute()
                }
            }
            if (event.message.content.equals("-stop", true)) {
                if (event.author.longID == OWNER_ID) {
                    try {
                        RequestBuffer.request { event.message.delete() }
                    } catch (e: MissingPermissionsException) {
                    }
                    for (leaderboard in guilds) {
                        leaderboard.saveLeaderboard()
                    }
                    cli.logout()
                    running = false
                    System.exit(0)
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
        changeStatus(StatusType.ONLINE, ActivityType.LISTENING, "the rain -help")
    }


}
