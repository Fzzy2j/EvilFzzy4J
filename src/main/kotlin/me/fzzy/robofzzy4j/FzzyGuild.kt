package me.fzzy.robofzzy4j

import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
import discord4j.core.`object`.entity.Guild
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.MessageChannel
import discord4j.core.`object`.entity.User
import discord4j.core.`object`.util.Snowflake
import me.fzzy.robofzzy4j.listeners.VoteListener
import me.fzzy.robofzzy4j.util.LavaPlayerAudioProvider
import me.fzzy.robofzzy4j.util.MediaType
import java.io.*
import java.util.*
import kotlin.math.roundToInt

class FzzyGuild private constructor(private var guildId: Snowflake) {

    companion object {
        private val guilds = arrayListOf<FzzyGuild>()

        fun getGuild(guildId: Snowflake): FzzyGuild {
            for (guild in guilds) {
                if (guild.id == guildId)
                    return guild
            }
            val guildFile = File(Bot.DATA_FILE, "$guildId.json")
            val guild = if (guildFile.exists()) {
                Bot.gson.fromJson(JsonReader(InputStreamReader(guildFile.inputStream())), FzzyGuild::class.java)
            } else FzzyGuild(guildId)
            guilds.add(guild)
            return guild
        }

        fun getGuild(guild: Guild): FzzyGuild {
            return getGuild(guild.id)
        }

        fun saveAll() {
            for (guild in guilds) {
                guild.save()
            }
        }
    }

    @Expose
    val id: Snowflake = this.guildId
    @Expose
    private var posts = 0
    @Expose
    private var votes = 0
    @Expose
    var currency: HashMap<Snowflake, Int> = hashMapOf()
    @Expose
    val savedMessageIds = ArrayList<Snowflake>()

    val player = LavaPlayerAudioProvider(Bot.playerManager.createPlayer())

    fun allowVotes(msg: Message) {
        posts++
        votes++
        msg.addReaction(Bot.CURRENCY_EMOJI).block()
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
        if (!savedMessageIds.contains(message.id)) {
            savedMessageIds.add(message.id)

            val url = Bot.getMessageMediaUrl(message, MediaType.IMAGE_AND_VIDEO).block() ?: return null
            val suffixFinder = url.toString().split(".")
            val suffix = ".${suffixFinder[suffixFinder.size - 1]}"

            File("memes", guildId.toString()).mkdirs()
            val fileName = "memes/$guildId/${System.currentTimeMillis()}.$suffix"
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
        currency[id] = currency.getOrDefault(id, 0) + amount
        if (message != null && VoteListener.getVotes(message) > getAverageVote()) {
            saveMessage(message)
        }
    }

    fun getCurrency(user: User): Int {
        return currency.getOrDefault(user.id, 0)
    }

    fun getCurrency(user: FzzyUser): Int {
        return currency.getOrDefault(user.id, 0)
    }

    fun getAverageVote(): Int {
        if (posts == 0) return 0
        return (votes.toFloat() / posts.toFloat()).roundToInt()
    }

    fun getDiscordGuild(): Guild {
        return Bot.client.getGuildById(guildId).block()!!
    }

    fun save() {
        val guildFile = File(Bot.DATA_FILE, "$guildId.json")
        val bufferWriter = BufferedWriter(FileWriter(guildFile.absoluteFile, false))
        val save = Bot.gson.toJson(this)
        bufferWriter.write(save)
        bufferWriter.close()
    }

}