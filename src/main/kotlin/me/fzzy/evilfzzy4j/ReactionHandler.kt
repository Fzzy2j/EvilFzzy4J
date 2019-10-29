package me.fzzy.evilfzzy4j

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionRemoveEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

object ReactionHandler : ListenerAdapter() {

    fun getVotes(message: Message): Int {
        return getUpvoters(message).size
    }

    fun getUpvoters(message: Message): List<User> {
        val list = arrayListOf<User>()

        val reaction = message.retrieveReactionUsers(Bot.currencyEmoji)
        for (user in reaction.complete()) {
            if (user.idLong != message.author.idLong && user.idLong != Bot.client.selfUser.idLong) list.add(user)
        }
        return list
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.author.isBot) {
            if (event.isFromGuild) {
                val guild = FzzyGuild.getGuild(event.guild.id)
                if (Bot.URL_PATTERN.matcher(event.message.contentRaw).find() || event.message.attachments.size > 0) {
                    guild.allowVotes(event.message)
                }
            }
        }
    }

    override fun onGuildMessageReactionAdd(event: GuildMessageReactionAddEvent) {
        if (event.user.idLong == Bot.client.selfUser.idLong) return
        if (event.user.idLong == event.channel.retrieveMessageById(event.messageId).complete().author.idLong) return

        val guild = FzzyGuild.getGuild(event.guild.id)
        val msg = event.channel.getHistoryBefore(event.channel.latestMessageId, 10).complete().getMessageById(event.messageId) ?: return

        val reaction = msg.retrieveReactionUsers(Bot.currencyEmoji)

        reaction.takeUntilAsync { user ->
            if (user.idLong == Bot.client.selfUser.idLong) {
                guild.addCurrency(msg.author, 1, msg)
                true
            } else false
        }
    }

    override fun onGuildMessageReactionRemove(event: GuildMessageReactionRemoveEvent) {
        if (event.user.idLong == Bot.client.selfUser.idLong) return
        if (event.user.idLong == event.channel.retrieveMessageById(event.messageId).complete().author.idLong) return

        val guild = FzzyGuild.getGuild(event.guild.id)
        val msg = event.channel.getHistoryBefore(event.channel.latestMessageId, 10).complete().getMessageById(event.messageId)
                ?: return

        val reaction = msg.retrieveReactionUsers(Bot.currencyEmoji)

        reaction.takeUntilAsync { user ->
            if (user.idLong == Bot.client.selfUser.idLong) {
                guild.addCurrency(msg.author, -1, msg)
                true
            } else false
        }
    }

}