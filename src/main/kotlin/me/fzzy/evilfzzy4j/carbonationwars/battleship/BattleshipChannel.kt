package me.fzzy.evilfzzy4j.carbonationwars.battleship

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
import me.fzzy.evilfzzy4j.Bot
import me.fzzy.evilfzzy4j.FzzyGuild
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.TimeUnit

class BattleshipChannel constructor(private val channelId: Long, reward: Int = 5) : ListenerAdapter() {

    var channel: TextChannel? = null

    private class Data constructor(
            @Expose var reward: Int,
            @Expose var dayOfWeek: Int,
            @Expose var boardMsg: Long = 0,
            @Expose var cdMsg: Long = 0,
            @Expose val cooldowns: ArrayList<Long> = arrayListOf(),
            @Expose var board: BattleshipBoard = BattleshipBoard()
    )

    private val cal = Calendar.getInstance()
    private var data: Data

    init {
        cal.time = Date()
        data = Data(reward, cal.get(Calendar.DAY_OF_MONTH))
        val gameFile = File("games${File.separator}battleship${channelId}.json")
        if (gameFile.exists()) data = Bot.gson.fromJson(JsonReader(InputStreamReader(gameFile.inputStream())), Data::class.java)

        Bot.scheduler.schedulePeriodically({
            if (channel != null) {
                cal.time = Date()
                if (data.dayOfWeek != cal.get(Calendar.DAY_OF_MONTH)) {
                    data.dayOfWeek = cal.get(Calendar.DAY_OF_MONTH)
                    data.cooldowns.clear()
                    refreshCooldowns()
                    save()
                }
                if (data.board.allAttacked()) {
                    data.board = BattleshipBoard()
                    refreshGame()
                }
            }
        }, 5, 10, TimeUnit.SECONDS)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        if (event.channel.idLong != channelId || event.author.isBot) return

        event.message.delete().queue()

        if (event.message.contentRaw.length >= 2 && !isOnCooldown(event.author.idLong)) {
            try {
                if (data.board.attack(event.author.idLong, event.message.contentRaw[0].toLowerCase(), event.message.contentRaw.substring(1).toInt())) {
                    val guild = FzzyGuild.getGuild(event.guild.id)
                    guild.addCurrency(event.author.idLong, data.reward)
                }
                Bot.logger.info("[Battleship] ${event.member?.effectiveName} attacking ${event.message.contentRaw[0].toLowerCase()}${event.message.contentRaw.substring(1)}")
                data.cooldowns.add(event.author.idLong)
                refreshGame()
                refreshCooldowns()
                save()
            } catch (e: NumberFormatException) {
            }
        }
    }

    override fun onReady(event: ReadyEvent) {
        channel = Bot.client.getTextChannelById(channelId)
        channel?.iterableHistory?.forEach { m -> if (m.idLong != data.boardMsg && m.idLong != data.cdMsg) m.delete().queue() }
        refreshGame()
    }

    fun isOnCooldown(id: Long): Boolean {
        return data.cooldowns.contains(id)
    }

    fun getCooldownText(): String {
        if (channel == null) return ""
        val builder = StringBuilder()
        for (id in data.cooldowns) {
            builder.append("${channel!!.guild.getMemberById(id)!!.effectiveName}\n")
        }
        return builder.toString()
    }

    fun refreshGame() {
        channel?.retrieveMessageById(data.boardMsg)?.queue({ msg ->
            run {
                msg?.editMessage(data.board.getBoardAsText())?.queue()
            }
        }, {
            channel?.sendMessage(data.board.getBoardAsText())?.queue { newMsg ->
                run {
                    data.boardMsg = newMsg.idLong
                    save()
                }
            }
        })
    }

    fun refreshCooldowns() {
        channel?.retrieveMessageById(data.cdMsg)?.queue({ msg ->
            run {
                val cooldownText = getCooldownText()

                if (msg.contentRaw != cooldownText) {
                    if (cooldownText.isEmpty())
                        msg.delete().queue()
                    else
                        msg.editMessage(getCooldownText()).queue()
                }
            }
        }, {
            if (getCooldownText().isNotEmpty()) {
                channel?.sendMessage(getCooldownText())?.queue { msg ->
                    run {
                        data.cdMsg = msg.idLong
                        save()
                    }
                }
            }
        })
    }

    fun save() {
        File("games").mkdirs()
        val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
        val gameFile = File("games${File.separator}battleship${channelId}.json")
        val bufferWriter = BufferedWriter(FileWriter(gameFile.absoluteFile, false))
        val save = JSONObject(gson.toJson(data))
        bufferWriter.write(save.toString(2))
        bufferWriter.close()
    }

}