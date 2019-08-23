package me.fzzy.evilfzzy4j.carbonationwars.battleship

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
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
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

class BattleshipChannel constructor(private val channelId: Snowflake, cooldown: Int = 1000 * 60 * 60, reward: Int = 2) {

    lateinit var channel: TextChannel

    private class Data constructor(
            @Expose var boardMsg: Long = 0,
            @Expose var cdMsg: Long = 0,
            @Expose val cooldowns: HashMap<Long, Long> = hashMapOf(),
            @Expose var board: BattleshipBoard = BattleshipBoard(),
            @Expose var reward: Int = 2,
            @Expose var cooldown: Int = 1000 * 60 * 60
    )

    private var data = Data(reward = reward, cooldown = cooldown)

    init {
        val gameFile = File("games${File.separator}battleship${channelId.asLong()}.json")
        if (gameFile.exists()) data = Bot.gson.fromJson(JsonReader(InputStreamReader(gameFile.inputStream())), Data::class.java)

        Bot.client.eventDispatcher.on(ReadyEvent::class.java)
                .map { event -> event.guilds.size }
                .flatMap { size ->
                    Bot.client.eventDispatcher.on(GuildCreateEvent::class.java)
                            .take(size.toLong())
                            .collectList()
                }.subscribe {
                    channel = Bot.client.getChannelById(channelId).block()!! as TextChannel
                    refreshGame()
                    channel.getMessagesBefore(Snowflake.of(data.boardMsg)).subscribe { msg -> msg.delete().block() }
                }

        Bot.client.eventDispatcher.on(MessageCreateEvent::class.java).subscribe {
            if (it.message.channelId == channelId && !it.message.author.get().isBot) {
                if (it.message.content.get().length >= 2 && !isOnCooldown(it.member.get().id)) {
                    try {
                        if (data.board.attack(it.member.get(), it.message.content.get()[0].toLowerCase(), it.message.content.get().substring(1).toInt())) {
                            val guild = FzzyGuild.getGuild(it.member.get().guildId)
                            guild.addCurrency(it.member.get(), reward)
                        }
                        Bot.logger.info("[Battleship] ${it.member.get().displayName} attacking ${it.message.content.get()[0].toLowerCase()}${it.message.content.get().substring(1)}")
                        data.cooldowns[it.member.get().id.asLong()] = System.currentTimeMillis()
                        refreshGame()
                        save()
                    } catch (e: NumberFormatException) {
                    }
                }
                it.message.delete().block()
            }
        }

        Bot.scheduler.schedulePeriodically({
            refreshCooldowns()
            if (data.board.allAttacked()) {
                data.board = BattleshipBoard()
                refreshGame()
            }
        }, 5, 10, TimeUnit.SECONDS)
    }

    fun isOnCooldown(id: Snowflake): Boolean {
        if (data.cooldowns.containsKey(id.asLong())) {
            if (System.currentTimeMillis() < data.cooldowns[id.asLong()]!! + data.cooldown)
                return true
        }
        return false
    }

    fun getCooldownText(): String {
        val builder = StringBuilder()
        val iter = data.cooldowns.iterator()
        while (iter.hasNext()) {
            val e = iter.next()
            val id = e.key
            val time = e.value
            val member = channel.guild.block()!!.getMemberById(Snowflake.of(id)).block()
            if (member == null) {
                iter.remove()
                continue
            }
            if (System.currentTimeMillis() > time + data.cooldown) {
                iter.remove()
                continue
            }

            val percentage = (((time + data.cooldown) - System.currentTimeMillis()) / data.cooldown.toDouble()) * 100
            builder.append(ProgressBar.getBar(percentage.roundToInt()))
            builder.append(" ${member.displayName}\n")
        }
        return builder.toString()
    }

    fun refreshGame() {
        try {
            channel.getMessageById(Snowflake.of(data.boardMsg)).block()!!.edit { spec -> spec.setContent(data.board.getBoardAsText()) }.block()
        } catch (e: Exception) {
            data.boardMsg = channel.createMessage(data.board.getBoardAsText()).block()!!.id.asLong()
            save()
        }
    }

    fun refreshCooldowns() {
        try {
            val msg = channel.getMessageById(Snowflake.of(data.cdMsg)).block()!!
            val cooldownText = getCooldownText()
            if (!msg.content.get().equals(cooldownText)) {
                if (cooldownText.isEmpty())
                    msg.delete().block()
                else
                    msg.edit { spec -> spec.setContent(getCooldownText()) }.block()!!.id.asLong()
            }
        } catch (e: Exception) {
            if (getCooldownText().isNotEmpty()) {
                data.cdMsg = channel.createMessage(getCooldownText()).block()!!.id.asLong()
                save()
            }
        }
    }

    fun save() {
        File("games").mkdirs()
        val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
        val gameFile = File("games${File.separator}battleship${channelId.asLong()}.json")
        val bufferWriter = BufferedWriter(FileWriter(gameFile.absoluteFile, false))
        val save = JSONObject(gson.toJson(data))
        bufferWriter.write(save.toString(2))
        bufferWriter.close()
    }

}