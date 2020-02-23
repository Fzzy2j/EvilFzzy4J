package me.fzzy.evilfzzy4j

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import org.json.JSONObject
import java.io.*
import java.util.*
import kotlin.math.roundToInt

class FzzyGuild private constructor() {

    companion object {
        private val guilds = arrayListOf<FzzyGuild>()

        fun getGuild(guildId: String): FzzyGuild {
            for (guild in guilds) {
                if (guild.guildId == guildId)
                    return guild
            }
            val guildFile = File(Bot.DATA_FILE, "${guildId}.json")
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

        fun lerpScores(amount: Float) {
            for (guild in guilds) {
                for ((id, score) in guild.currency) {
                    guild.currency[id] = (score * amount).roundToInt()
                }
            }
        }
    }

    private lateinit var guildId: String

    @Expose
    private var posts = 0
    @Expose
    private var votes = 0
    @Expose
    var currency: HashMap<Long, Int> = hashMapOf()
    @Expose
    private val savedMessageIds = ArrayList<Long>()

    //val player = FzzyPlayer(LavaPlayerAudioProvider(Bot.playerManager.createPlayer()))

    fun allowVotes(msg: Message) {
        posts++
        votes++
        msg.addReaction(Bot.currencyEmoji).queue()
    }

    fun getSortedCurrency(): Map<Long, Int> {
        return currency.toList().sortedBy { (key, value) -> -value }.toMap()
    }

    fun sendVoteAttachment(file: File, channel: TextChannel, credit: User? = null): Message? {
        val msg = if (credit != null) {
            channel.sendMessage(credit.asMention).addFile(file).complete()
        } else {
            channel.sendFile(file).complete()
        }
        if (msg != null) allowVotes(msg)
        return msg
    }

    fun saveMessage(message: Message): File? {
        if (!savedMessageIds.contains(message.idLong)) {
            savedMessageIds.add(message.idLong)

            val url = Bot.getMessageMedia(message) ?: return null
            val suffixFinder = url.toString().split(".")
            val suffix = suffixFinder[suffixFinder.size - 1]

            File("memes", guildId).mkdirs()
            val fileName = "memes/${guildId}/${System.currentTimeMillis()}.$suffix"
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
        addCurrency(user.idLong, amount, message)
    }

    fun addCurrency(fzzyUser: FzzyUser, amount: Int, message: Message? = null) {
        addCurrency(fzzyUser.id, amount, message)
    }

    fun addCurrency(id: Long, amount: Int, message: Message? = null) {
        votes += amount
        currency[id] = currency.getOrDefault(id, 0) + amount
        if (message != null && ReactionHandler.getVotes(message) > getAverageVote()) {
            saveMessage(message)
        }

    }

    fun getCurrency(user: User): Int {
        return currency.getOrDefault(user.idLong, 0)
    }

    fun getCurrency(user: FzzyUser): Int {
        return currency.getOrDefault(user.id, 0)
    }

    fun getAverageVote(): Int {
        if (posts == 0) return 0
        return (votes.toFloat() / posts.toFloat()).roundToInt()
    }

    fun getDiscordGuild(): Guild? {
        return Bot.client.getGuildById(guildId)
    }

    fun save() {
        val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
        val guildFile = File(Bot.DATA_FILE, "${guildId}.json")
        val bufferWriter = BufferedWriter(FileWriter(guildFile.absoluteFile, false))
        val save = JSONObject(gson.toJson(this))
        bufferWriter.write(save.toString(2))
        bufferWriter.close()
    }

}