package me.fzzy.robofzzy4j.listeners

import me.fzzy.robofzzy4j.*
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageDeleteEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageSendEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.RequestBuilder
import java.lang.NullPointerException
import java.util.regex.Matcher
import java.util.regex.Pattern

class MessageListener {

    @EventSubscriber
    fun onMessageSend(event: MessageSendEvent) {
        if (event.guild != null) {
            if (event.message.attachments.size > 0) {
                Guild.getGuild(event.guild.longID).allowVotes(event.message)
            }
        }
    }

    val respongeMsgs = listOf(
            "No problem %name%!",
            "Anytime %name%!",
            ":D",
            ":3",
            ":^)"
    )

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {
        if (Funcs.mentionsByName(event.message)) {
            for (msg in event.channel.getMessageHistory(3)) {
                if (msg.author.longID == cli.ourUser.longID) {
                    RequestBuffer.request {
                        event.channel.sendMessage(respongeMsgs[random.nextInt(respongeMsgs.size)].replace("%name%", event.author.getDisplayName(event.guild)))
                    }
                    break
                }
            }
        }
        if (event.guild != null) {
            if (!event.author.isBot) {
                val guild = Guild.getGuild(event.guild.longID)
                val pattern = Pattern.compile("((http:\\/\\/|https:\\/\\/)?(www.)?(([a-zA-Z0-9-]){2,}\\.){1,4}([a-zA-Z]){2,6}(\\/([a-zA-Z-_\\/\\.0-9#:?=&;,]*)?)?)")
                val m: Matcher = pattern.matcher(event.message.content)
                if (m.find() || event.message.attachments.size > 0) {
                    guild.allowVotes(event.message)
                }
            }
        }
    }
}