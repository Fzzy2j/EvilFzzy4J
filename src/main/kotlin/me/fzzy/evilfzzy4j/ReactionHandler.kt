package me.fzzy.evilfzzy4j

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object ReactionHandler : ListenerAdapter() {

    override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
        if (!event.reactionEmote.isEmote) return
        if (event.user.idLong == Bot.client.selfUser.idLong) return
        if (event.reactionEmote.idLong != Bot.upvoteEmote.idLong && event.reactionEmote.idLong != Bot.downvoteEmote.idLong) return
        val msg = event.channel.retrieveMessageById(event.messageId).complete() ?: return
        //if (event.user.idLong == msg.author.idLong) return

        val guild = FzzyGuild.getGuild(event.guild.id)
        if (event.reactionEmote.idLong == Bot.upvoteEmote.idLong) {
            guild.saveMessage(msg)
            guild.incrementScore(msg.author.idLong)
        } else {
            guild.decrementScore(msg.author.idLong)
        }
    }

    override fun onGuildMessageReactionRemove(event: GuildMessageReactionRemoveEvent) {
        if (!event.reactionEmote.isEmote) return
        if (event.user.idLong == Bot.client.selfUser.idLong) return
        if (event.reactionEmote.idLong != Bot.upvoteEmote.idLong && event.reactionEmote.idLong != Bot.downvoteEmote.idLong) return
        val msg = event.channel.retrieveMessageById(event.messageId).complete() ?: return
        //if (event.user.idLong == msg.author.idLong) return

        val guild = FzzyGuild.getGuild(event.guild.id)
        if (event.reactionEmote.idLong == Bot.upvoteEmote.idLong) {
            guild.decrementScore(msg.author.idLong)
        } else {
            guild.incrementScore(msg.author.idLong)
        }
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val matcher = Bot.URL_PATTERN.matcher(event.message.contentRaw)
        if (matcher.find() || event.message.attachments.isNotEmpty()) {
            event.message.addReaction(Bot.upvoteEmote).queue { event.message.addReaction(Bot.downvoteEmote).queue() }
        }
    }

}