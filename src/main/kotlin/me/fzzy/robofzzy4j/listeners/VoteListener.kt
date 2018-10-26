package me.fzzy.robofzzy4j.listeners

import me.fzzy.robofzzy4j.*
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionRemoveEvent
import sx.blah.discord.handle.obj.IMessage

object VoteListener {

    fun getVotes(message: IMessage): Int {
        var upvotes = 0
        for (user in message.getReactionByEmoji(UPVOTE_EMOJI).users) {
            if (user.longID != message.author.longID) upvotes++
        }
        var downvotes = 0
        for (user in message.getReactionByEmoji(DOWNVOTE_EMOJI).users) {
            if (user.longID != message.author.longID) downvotes++
        }
        return upvotes - downvotes
    }

    @EventSubscriber
    fun onReactionAdd(event: ReactionAddEvent) {
        val guild = Guild.getGuild(event.guild.longID)
        if (event.reaction != null) {
            if (event.reaction.getUserReacted(cli.ourUser) && event.user.longID != cli.ourUser.longID) {
                if (event.channel.getMessageHistory(10).contains(event.message)) {
                    if (event.message.author.longID != event.user.longID) {
                        when (event.reaction.emoji.name) {
                            "upvote" -> {
                                guild.addPoint(event.message, event.author, event.channel)
                            }
                            "downvote" -> {
                                guild.subtractPoint(event.author, event.channel)
                            }
                        }
                    }
                }
            }
        }
    }

    @EventSubscriber
    fun onReactionRemove(event: ReactionRemoveEvent) {
        val guild = Guild.getGuild(event.guild.longID)
        if (event.channel.getMessageHistory(10).contains(event.message)) {
            if (event.reaction.getUserReacted(cli.ourUser) && event.user.longID != cli.ourUser.longID) {
                if (event.message.author.longID != event.user.longID) {
                    when (event.reaction.emoji.name) {
                        "upvote" -> {
                            guild.subtractPoint(event.author, event.channel)
                        }
                        "downvote" -> {
                            guild.addPoint(event.message, event.author, event.channel)
                        }
                    }
                }
            }
        }
    }
}