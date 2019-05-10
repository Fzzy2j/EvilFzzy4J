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
        return getUpvoters(message).size
    }

    fun getUpvoters(message: IMessage): List<IUser> {
        val list = arrayListOf<IUser>()
        for (user in message.getReactionByEmoji(Bot.CURRENCY_EMOJI).users) {
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
                    if (event.reaction.emoji.longID == Bot.CURRENCY_EMOJI.longID) {
                        guild.addCurrency(event.author, 1, event.message)
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
                    if (event.reaction.emoji.longID == Bot.CURRENCY_EMOJI.longID) {
                        guild.addCurrency(event.author, -1)
                    }
                }
            }
        }
    }
}