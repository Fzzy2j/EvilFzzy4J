package me.fzzy.evilfzzy4j

import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object ReactionHandler : ListenerAdapter() {

    override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
        if (event.user.idLong == Bot.client.selfUser.idLong) return
        if (event.user.idLong == event.channel.retrieveMessageById(event.messageId).complete().author.idLong) return

        val guild = FzzyGuild.getGuild(event.guild.id)
        val msg = event.channel.getHistoryBefore(event.channel.latestMessageId, 10).complete().getMessageById(event.messageId) ?: return

        guild.saveMessage(msg)
    }

}