package me.fzzy.robofzzy4j

import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
import me.fzzy.robofzzy4j.listeners.VoteListener
import me.fzzy.robofzzy4j.util.MediaType
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.io.*
import java.util.*
import kotlin.math.roundToInt

class Guild private constructor(private var guildId: Long) {

    companion object {
        private val guilds = arrayListOf<Guild>()

        fun getGuild(guildId: Long): Guild {
            for (guild in guilds) {
                if (guild.longId == guildId)
                    return guild
            }
            val guildFile = File("${Bot.DATA_DIR}guilds${File.separator}$guildId.json")
            val guild = if (guildFile.exists()) {
                Bot.gson.fromJson<Guild>(JsonReader(InputStreamReader(guildFile.inputStream())), Guild::class.java)
            } else Guild(guildId)
            guilds.add(guild)
            return guild
        }

        fun getGuild(guild: IGuild): Guild {
            return getGuild(guild.longID)
        }

        fun saveAll() {
            for (guild in guilds) {
                guild.save()
            }
        }
    }

    @Expose
    val longId: Long = this.guildId
    @Expose
    private var posts = 0
    @Expose
    private var votes = 0
    @Expose
    var currency: HashMap<Long, Int> = hashMapOf()
    @Expose
    val savedMessageIds = ArrayList<Long>()

    fun allowVotes(msg: IMessage) {
        posts++
        votes++
        RequestBuffer.request {
            try {
                msg.addReaction(Bot.CURRENCY_EMOJI)
            } catch (e: Exception) {
            }
        }
    }

    fun sendVoteAttachment(file: File, channel: IChannel, credit: IUser? = null): IMessage? {
        val msg = if (credit != null) {
            try {
                channel.sendFile(credit.mention(), file)
            } catch (e: MissingPermissionsException) {
                null
            }
        } else {
            try {
                channel.sendFile(file)
            } catch (e: MissingPermissionsException) {
                null
            }
        }
        if (msg != null) allowVotes(msg)
        return msg
    }

    fun saveMessage(message: IMessage): File? {
        if (!savedMessageIds.contains(message.longID)) {
            savedMessageIds.add(message.longID)

            val url = Bot.getMessageMediaUrl(message, MediaType.IMAGE_AND_VIDEO) ?: return null
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

    fun addCurrency(user: IUser, amount: Int, message: IMessage? = null) {
        addCurrency(user.longID, amount, message)
    }

    fun addCurrency(user: User, amount: Int, message: IMessage? = null) {
        addCurrency(user.id, amount, message)
    }

    fun addCurrency(id: Long, amount: Int, message: IMessage? = null) {
        votes += amount
        currency[id] = currency.getOrDefault(id, 0) + amount
        if (message != null && VoteListener.getVotes(message) > getAverageVote()) {
            saveMessage(message)
        }
    }

    fun getCurrency(user: IUser): Int {
        return currency.getOrDefault(user.longID, 0)
    }

    fun getCurrency(user: User): Int {
        return currency.getOrDefault(user.id, 0)
    }

    fun getAverageVote(): Int {
        if (posts == 0) return 0
        return (votes.toFloat() / posts.toFloat()).roundToInt()
    }

    fun getDiscordGuild(): IGuild {
        return Bot.client.getGuildByID(longId)
    }

    fun save() {
        File("${Bot.DATA_DIR}guilds").mkdirs()
        val guildFile = File("${Bot.DATA_DIR}guilds${File.separator}$guildId.json")
        val bufferWriter = BufferedWriter(FileWriter(guildFile.absoluteFile, false))
        val save = Bot.gson.toJson(this)
        bufferWriter.write(save)
        bufferWriter.close()
    }

}