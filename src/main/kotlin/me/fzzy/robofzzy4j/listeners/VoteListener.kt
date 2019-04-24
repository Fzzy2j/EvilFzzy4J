package me.fzzy.robofzzy4j.listeners

import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.Guild
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionRemoveEvent
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IUser

object VoteListener {

    fun getVotes(message: IMessage): Int {
        var upvotes = 0
        for (user in message.getReactionByEmoji(Bot.UPVOTE_EMOJI).users) {
            if (user.longID != message.author.longID) upvotes++
        }
        var downvotes = 0
        for (user in message.getReactionByEmoji(Bot.DOWNVOTE_EMOJI).users) {
            if (user.longID != message.author.longID) downvotes++
        }
        return upvotes - downvotes
    }

    fun getUpvoters(message: IMessage): List<IUser> {
        val list = arrayListOf<IUser>()
        for (user in message.getReactionByEmoji(Bot.UPVOTE_EMOJI).users) {
            if (user.longID != message.author.longID) list.add(user)
        }
        return list
    }

    fun getDownvoters(message: IMessage): List<IUser> {
        val list = arrayListOf<IUser>()
        for (user in message.getReactionByEmoji(Bot.DOWNVOTE_EMOJI).users) {
            if (user.longID != message.author.longID) list.add(user)
        }
        return list
    }

    @EventSubscriber
    fun onReactionAdd(event: ReactionAddEvent) {
        val guild = Guild.getGuild(event.guild.longID)
        if (event.channel.getMessageHistory(10).contains(event.message)) {
            val users = try {
                event.reaction.users
            } catch (e: NullPointerException) {
                return
            }
            if (users.contains(Bot.client.ourUser) && event.user.longID != Bot.client.ourUser.longID) {
                if (event.message.author.longID != event.user.longID) {
                    when (event.reaction.emoji.name) {
                        "upvote" -> {
                            guild.adjustScore(event.author, 1, event.channel, event.message)
                        }
                        "downvote" -> {
                            guild.adjustScore(event.author, -1, event.channel, event.message)
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
            if (event.reaction.getUserReacted(Bot.client.ourUser) && event.user.longID != Bot.client.ourUser.longID) {
                if (event.message.author.longID != event.user.longID) {
                    when (event.reaction.emoji.name) {
                        "upvote" -> {
                            guild.adjustScore(event.author, -1, event.channel, event.message)
                        }
                        "downvote" -> {
                            guild.adjustScore(event.author, 1, event.channel, event.message)
                        }
                    }
                }
            }
        }
    }
}