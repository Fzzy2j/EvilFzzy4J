package me.fzzy.evilfzzy4j

import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.User
import discord4j.core.event.EventDispatcher
import discord4j.core.event.domain.message.ReactionAddEvent
import discord4j.core.event.domain.message.ReactionRemoveEvent

object ReactionHandler {

    fun getVotes(message: Message): Int {
        return getUpvoters(message).size
    }

    fun getUpvoters(message: Message): List<User> {
        val list = arrayListOf<User>()

        for (user in message.getReactors(Bot.currencyEmoji).collectList().block()!!) {
            if (user.id != message.author.get().id && user.id != Bot.client.self.block()!!.id) list.add(user)
        }
        return list
    }

    fun registerEvents(dispatch: EventDispatcher) {
        dispatch.on(ReactionAddEvent::class.java).subscribe(ReactionHandler::onReactionAdd)
        dispatch.on(ReactionRemoveEvent::class.java).subscribe(ReactionHandler::onReactionRemove)
    }

    fun onReactionAdd(event: ReactionAddEvent) {
        if (event.userId == Bot.client.selfId.get()) return
        if (event.userId == event.message.block()!!.author.get().id) return

        val channel = event.channel.block()!!
        val guild = FzzyGuild.getGuild(event.guild.block()!!.id)
        val msg = channel.getMessagesBefore(channel.lastMessageId.get()).take(10).takeUntil { it.id == event.messageId }.next().block()
                ?: return

        val reaction = msg.getReactors(Bot.currencyEmoji).collectList().block() ?: return
        if (reaction.contains(Bot.client.self.block()!!)) {
            guild.addCurrency(msg.author.get(), 1, msg)
        }
    }

    fun onReactionRemove(event: ReactionRemoveEvent) {
        if (event.userId == Bot.client.selfId.get()) return
        if (event.userId == event.message.block()!!.author.get().id) return

        val channel = event.channel.block()!!
        val guild = FzzyGuild.getGuild(event.guild.block()!!.id)
        val msg = channel.getMessagesBefore(channel.lastMessageId.get()).take(10).takeUntil { it.id == event.messageId }.next().block()
                ?: return

        val reaction = msg.getReactors(Bot.currencyEmoji).collectList().block() ?: return
        if (reaction.contains(Bot.client.self.block()!!)) {
            guild.addCurrency(msg.author.get(), -1)
        }
    }
}