package me.fzzy.robofzzy4j

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.MessageChannel
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.util.Snowflake
import me.fzzy.robofzzy4j.voice.FzzyPlayer
import me.fzzy.robofzzy4j.voice.LavaPlayerAudioProvider
import org.json.JSONObject
import java.io.*
import java.util.*
import kotlin.math.roundToInt

class FzzyGuild private constructor() {

    companion object {
        private val guilds = arrayListOf<FzzyGuild>()

        fun getGuild(guildId: Snowflake): FzzyGuild {
            for (guild in guilds) {
                if (guild.guildId == guildId)
                    return guild
            }
            val guildFile = File(Bot.DATA_FILE, "${guildId.asString()}.json")
            val guild = if (guildFile.exists()) {
                    Bot.gson.fromJson(JsonReader(InputStreamReader(guildFile.inputStream())), FzzyGuild::class.java)
            } else FzzyGuild()
            guild.guildId = guildId
            guilds.add(guild)
            return guild
        }

        fun saveAll() {
            for (guild in guilds) {
                guild.save()
            }
        }
    }

    private lateinit var guildId: Snowflake

    @Expose
    private var posts = 0
    @Expose
    private var votes = 0
    @Expose
    private var currency: HashMap<Long, Int> = hashMapOf()
    @Expose
    private val savedMessageIds = ArrayList<Long>()

    val player = FzzyPlayer(LavaPlayerAudioProvider(Bot.playerManager.createPlayer()))

    fun allowVotes(msg: Message) {
        posts++
        votes++
        msg.addReaction(Bot.currencyEmoji).block()
    }

    fun getSortedCurrency(): SortedMap<Long, Int> {
        return currency.toSortedMap(compareBy { -currency[it]!! })
    }

    fun sendVoteAttachment(file: File, channel: MessageChannel, credit: User? = null): Message? {
        val msg = if (credit != null) {
            channel.createMessage { spec -> spec.setContent(credit.mention).addFile(file.name, file.inputStream()) }.block()
        } else {
            channel.createMessage { spec -> spec.addFile(file.name, file.inputStream()) }.block()
        }
        if (msg != null) allowVotes(msg)
        return msg
    }

    fun saveMessage(message: Message): File? {
        if (!savedMessageIds.contains(message.id.asLong())) {
            savedMessageIds.add(message.id.asLong())

            val url = Bot.getMessageMedia(message) ?: return null
            val suffixFinder = url.toString().split(".")
            val suffix = ".${suffixFinder[suffixFinder.size - 1]}"

            File("memes", guildId.asString()).mkdirs()
            val fileName = "memes/${guildId.asString()}/${System.currentTimeMillis()}.$suffix"
            try {
                val openConnection = url.openConnection()
                openConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
                openConnection.connect()

                val inputStream = BufferedInputStream(openConnection.getInputStream())
                val outputStream = BufferedOutputStream(FileOutputStream(fileName))

                for (out in inputStream.iterator()) {
                    outputStream.write(out.toInt())
                }
                inputStream.close()
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return File(fileName)
        }
        return null
    }

    fun addCurrency(user: User, amount: Int, message: Message? = null) {
        addCurrency(user.id, amount, message)
    }

    fun addCurrency(fzzyUser: FzzyUser, amount: Int, message: Message? = null) {
        addCurrency(fzzyUser.id, amount, message)
    }

    fun addCurrency(id: Snowflake, amount: Int, message: Message? = null) {
        votes += amount
        currency[id.asLong()] = currency.getOrDefault(id.asLong(), 0) + amount
        if (message != null && ReactionHandler.getVotes(message) > getAverageVote()) {
            saveMessage(message)
        }
    }

    fun getCurrency(user: User): Int {
        return currency.getOrDefault(user.id.asLong(), 0)
    }

    fun getCurrency(user: FzzyUser): Int {
        return currency.getOrDefault(user.id.asLong(), 0)
    }

    fun getAverageVote(): Int {
        if (posts == 0) return 0
        return (votes.toFloat() / posts.toFloat()).roundToInt()
    }

    fun getDiscordGuild(): Guild {
        return Bot.client.getGuildById(guildId).block()!!
    }

    fun save() {
        val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
        val guildFile = File(Bot.DATA_FILE, "${guildId.asString()}.json")
        val bufferWriter = BufferedWriter(FileWriter(guildFile.absoluteFile, false))
        val save = JSONObject(gson.toJson(this))
        bufferWriter.write(save.toString(2))
        bufferWriter.close()
    }

}