package me.fzzy.robofzzy4j.listeners

import me.fzzy.robofzzy4j.*
import me.fzzy.robofzzy4j.commands.LeaderboardCommand
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageDeleteEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEditEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageSendEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.util.EmbedBuilder
import sx.blah.discord.util.RequestBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher

object MessageListener {

    val respongeMsgs = listOf(
            "no problem %name%",
            "np %name%",
            ":D",
            ":P"
    )

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {
        if (Funcs.mentionsByName(event.message)) {
            for (msg in event.channel.getMessageHistory(7)) {
                if (msg.author.longID == Bot.client.ourUser.longID) {
                    RequestBuffer.request {
                        MessageScheduler.sendTempMessage(Bot.data.DEFAULT_TEMP_MESSAGE_DURATION, event.channel, respongeMsgs[Bot.random.nextInt(respongeMsgs.size)].replace("%name%", event.author.getDisplayName(event.guild).toLowerCase()))
                    }
                    break
                }
            }
        }
        if (event.guild != null) {
            if (!event.author.isBot) {
                val guild = Guild.getGuild(event.guild.longID)
                val m: Matcher = Bot.URL_PATTERN.matcher(event.message.content)
                if (m.find() || event.message.attachments.size > 0) {
                    guild.allowVotes(event.message)
                }
            }
        }
    }

    private const val gotchaId = 568977323513085973

    @EventSubscriber
    fun onMessageDelete(event: MessageDeleteEvent) {
        if (event.author == null || event.author.isBot) return
        if (CommandHandler.isCommand(event.message.content)) return
        val gotchaChannel = Bot.client.getChannelByID(gotchaId)

        val builder = EmbedBuilder()
        builder.appendField("Message", event.message.content, false)

        val date = SimpleDateFormat("hh:mm aa").format(Date(System.currentTimeMillis()))
        builder.withTitle("$date - ${event.author.name} deleted message")

        builder.withColor(0, 200, 255)
        builder.withThumbnail(event.author.avatarURL)
        RequestBuffer.request { gotchaChannel.sendMessage(builder.build()) }
    }

    @EventSubscriber
    fun onMessageEdit(event: MessageEditEvent) {
        if (event.author.isBot) return
        val gotchaChannel = Bot.client.getChannelByID(gotchaId)

        val builder = EmbedBuilder()
        builder.appendField("Original", event.oldMessage.content, false)
        builder.appendField("New", event.newMessage.content, false)

        val date = SimpleDateFormat("hh:mm aa").format(Date(System.currentTimeMillis()))
        builder.withTitle("$date - ${event.author.name} edited message")

        builder.withColor(0, 200, 255)
        builder.withThumbnail(event.author.avatarURL)
        RequestBuffer.request { gotchaChannel.sendMessage(builder.build()) }
    }

}