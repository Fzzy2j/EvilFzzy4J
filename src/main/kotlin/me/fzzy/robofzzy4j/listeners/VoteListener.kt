package me.fzzy.robofzzy4j.listeners

import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.FzzyGuild

object VoteListener {

    fun getVotes(message: IMessage): Int {
        return getUpvoters(message).size
    }

    fun getUpvoters(message: IMessage): List<IUser> {
        val list = arrayListOf<IUser>()
        for (user in message.getReactionByEmoji(Bot.CURRENCY_EMOJI).users) {
            if (user.longID != message.author.longID && user.longID != Bot.client.ourUser.longID) list.add(user)
        }
        return list
    }

    @EventSubscriber
    fun onReactionAdd(event: ReactionAddEvent) {
        val guild = FzzyGuild.getGuild(event.guild.longID)
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
        val guild = FzzyGuild.getGuild(event.guild.longID)
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