package me.fzzy.evilfzzy4j.carbonationwars.battleship

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import discord4j.core.`object`.entity.TextChannel
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.guild.GuildCreateEvent
import discord4j.core.event.domain.lifecycle.ReadyEvent
import discord4j.core.event.domain.message.MessageCreateEvent
import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import me.fzzy.evilfzzy4j.util.ProgressBar
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class BattleshipChannel constructor(private val channelId: Snowflake, var cooldown: Int = 1000 * 60 * 60, var reward: Int = 2){

    lateinit var channel: TextChannel

    @Expose
    var boardMsg: Long = 0
    @Expose
    var cdMsg: Long = 0

    @Expose
    val cooldowns = hashMapOf<Long, Long>()

    @Expose
    var board = Board()

    init {
        Bot.client.eventDispatcher.on(ReadyEvent::class.java)
                .map { event -> event.guilds.size }
                .flatMap { size ->
                    Bot.client.eventDispatcher.on(GuildCreateEvent::class.java)
                            .take(size.toLong())
                            .collectList()
                }.subscribe {
                    channel = Bot.client.getChannelById(channelId).block()!! as TextChannel
                    refreshGame()
                    channel.getMessagesBefore(Snowflake.of(boardMsg)).subscribe { msg -> msg.delete().block() }
                }

        Bot.client.eventDispatcher.on(MessageCreateEvent::class.java).subscribe {
            if (it.message.channelId == channelId && !it.message.author.get().isBot) {
                if (it.message.content.get().length >= 2 && !isOnCooldown(it.member.get().id)) {
                    try {
                        if (board.attack(it.member.get(), it.message.content.get()[0].toLowerCase(), it.message.content.get().substring(1).toInt())) {
                            val guild = FzzyGuild.getGuild(it.member.get().guildId)
                            guild.addCurrency(it.member.get(), reward)
                        }
                        Bot.logger.info("${it.member.get().displayName} attacking ${it.message.content.get()[0].toLowerCase()}${it.message.content.get().substring(1)}")
                        cooldowns[it.member.get().id.asLong()] = System.currentTimeMillis()
                        refreshGame()
                    } catch (e: NumberFormatException) {
                    }
                }
                it.message.delete().block()
            }
        }

        Bot.scheduler.schedulePeriodically({
            refreshCooldowns()
            if (board.allAttacked()) {
                board = Board()
                refreshGame()
            }
        }, 5, 10, TimeUnit.SECONDS)
    }

    fun isOnCooldown(id: Snowflake): Boolean {
        if (cooldowns.containsKey(id.asLong())) {
            if (System.currentTimeMillis() < cooldowns[id.asLong()]!! + cooldown)
                return true
        }
        return false
    }

    fun getCooldownText(): String {
        val builder = StringBuilder()
        val iter = cooldowns.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            val id = e.key
            val time = e.value
            val member = channel.guild.block()!!.getMemberById(Snowflake.of(id)).block()
            if (member == null) {
                iter.remove()
                continue
            }
            if (System.currentTimeMillis() > time + cooldown) {
                iter.remove()
                continue
            }

            val percentage = (((time + cooldown) - System.currentTimeMillis()) / cooldown.toDouble()) * 100
            builder.append(ProgressBar.getBar(percentage.roundToInt()))
            builder.append(" ${member.displayName}\n")
        }
        return builder.toString()
    }

    fun refreshGame() {
        try {
            channel.getMessageById(Snowflake.of(boardMsg)).block()!!.edit { spec -> spec.setContent(board.getBoardAsText()) }.block()
        } catch (e: Exception) {
            boardMsg = channel.createMessage(board.getBoardAsText()).block()!!.id.asLong()
        }
        save()
    }

    fun refreshCooldowns() {
        try {
            val msg = channel.getMessageById(Snowflake.of(cdMsg)).block()!!
            val cooldownText = getCooldownText()
            if (!msg.content.get().equals(cooldownText)) {
                if (cooldownText.isEmpty())
                    msg.delete().block()
                else {
                    cdMsg = msg.edit { spec -> spec.setContent(getCooldownText()) }.block()!!.id.asLong()
                    save()
                }
            }
        } catch (e: Exception) {
            if (getCooldownText().isNotEmpty()) {
                cdMsg = channel.createMessage(getCooldownText()).block()!!.id.asLong()
                save()
            }
        }
    }

    fun save() {
        val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
        val ranchFile = File("ranch.json")
        val bufferWriter = BufferedWriter(FileWriter(ranchFile.absoluteFile, false))
        val save = JSONObject(gson.toJson(this))
        bufferWriter.write(save.toString(2))
        bufferWriter.close()
    }

}