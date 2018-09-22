package me.fzzy.robofzzy4j.listeners

import me.fzzy.robofzzy4j.*
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageSendEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.RequestBuilder
import java.util.regex.Matcher
import java.util.regex.Pattern

class MessageListener {

    @EventSubscriber
    fun onMessageSend(event: MessageSendEvent) {
        if (event.guild != null) {
            if (event.message.attachments.size > 0) {
                getGuild(event.guild.longID)!!.posts++
                getGuild(event.guild.longID)!!.votes++
                RequestBuilder(event.client).shouldBufferRequests(true).doAction {
                    if (event.message != null)
                        event.message.addReaction(ReactionEmoji.of("upvote", 445376322353496064L))
                    true
                }.andThen {
                    if (event.message != null)
                        event.message.addReaction(ReactionEmoji.of("downvote", 445376330989830147L))
                    true
                }.execute()
            }
        }
    }

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.guild != null) {
            if (!event.author.isBot) {
                if (getGuild(event.guild.longID) == null)
                    guilds.add(Guild(event.guild.longID))
                val pattern = Pattern.compile("((http:\\/\\/|https:\\/\\/)?(www.)?(([a-zA-Z0-9-]){2,}\\.){1,4}([a-zA-Z]){2,6}(\\/([a-zA-Z-_\\/\\.0-9#:?=&;,]*)?)?)")
                val m: Matcher = pattern.matcher(event.message.content)
                if (m.find() || event.message.attachments.size > 0) {
                    getGuild(event.guild.longID)!!.posts++
                    getGuild(event.guild.longID)!!.votes++
                    RequestBuilder(event.client).shouldBufferRequests(true).doAction {
                        event.message.addReaction(ReactionEmoji.of("upvote", 445376322353496064L))
                        true
                    }.andThen {
                        event.message.addReaction(ReactionEmoji.of("downvote", 445376330989830147L))
                        true
                    }.execute()
                }
            }
        }
        if (!event.message.author.isBot) {
            if (event.message.content.equals("-stop", true)) {
                if (event.author.longID == OWNER_ID) {
                    RequestBuffer.request { event.message.delete() }
                    for (guild in guilds) {
                        guild.save()
                    }
                    cli.logout()
                    running = false
                    System.exit(0)
                }
            }
        }
    }
}