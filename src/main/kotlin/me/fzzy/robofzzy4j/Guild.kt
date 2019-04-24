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
import sx.blah.discord.util.RequestBuilder
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

        fun clearLeaderboards() {
            for (guild in guilds) {
                guild.leaderboard.clear()
            }
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
    var leaderboard: Leaderboard = Leaderboard()
    @Expose
    private var posts = 0
    @Expose
    private var votes = 0
    @Expose
    private var currency: HashMap<Long, Int> = hashMapOf()
    @Expose
    val savedMessageIds = ArrayList<Long>()

    private var changes: Leaderboard.LeaderboardChange? = null

    fun handleChanges(change: Leaderboard.LeaderboardChange, channel: IChannel) {
        if (change.positions.isEmpty())
            return

        if (changes == null) changes = change

        val messages = channel.getMessageHistory(5)
        var existingMessage: IMessage? = null

        for (history in messages) {
            if (history.content.startsWith("```diff\n--- Leaderboard Changes\n") && history.author.longID == Bot.client.ourUser.longID) {
                existingMessage = history
                break
            }
        }
        if (existingMessage == null) changes = change else changes?.add(change)

        var message = "```diff\n--- Leaderboard Changes\n"
        for ((id, amt) in sortHashMapByValues(changes!!.positions)) {
            if (amt == 0) continue
            val name = Bot.client.getUserByID(id).getDisplayName(getDiscordGuild())
            val rank = leaderboard.getRank(id)
            message += if (amt > 0)
                "+ $name #$rank CDR=${User.getUser(id).getCooldownModifier(this)}%\n"
            else
                "- $name #$rank CDR=${User.getUser(id).getCooldownModifier(this)}%\n"
        }
        message += "```"
        if (existingMessage != null)
            RequestBuffer.request { existingMessage.edit(message) }
        else
            RequestBuffer.request { MessageScheduler.sendTempMessage(Bot.data.DEFAULT_TEMP_MESSAGE_DURATION, channel, message) }
    }

    fun adjustScore(user: IUser, amount: Int, broadcastChannel: IChannel? = null, message: IMessage? = null) {
        val score = leaderboard.getOrDefault(user.longID, 0)
        val changes = if (!user.isBot) leaderboard.setValue(user.longID, score + amount) else Leaderboard.LeaderboardChange()

        if (!user.isBot && broadcastChannel != null)
            handleChanges(changes, broadcastChannel)
        else if (user.longID == Bot.client.ourUser.longID && message != null && message.mentions.isNotEmpty()) {
            for (mention in message.mentions) {
                adjustScore(mention, amount, broadcastChannel)
            }
        }
        votes += amount

        // Handle repost saving
        if (message != null && VoteListener.getVotes(message) > getAverageVote()) {
            saveMessage(message)
        }
    }

    fun allowVotes(msg: IMessage) {
        if (Bot.data.cooldownMode) {
            posts++
            votes++
            RequestBuilder(Bot.client).shouldBufferRequests(true).doAction {
                try {
                    msg.addReaction(Bot.UPVOTE_EMOJI)
                } catch (e: MissingPermissionsException) {
                }
                true
            }.andThen {
                try {
                    msg.addReaction(Bot.DOWNVOTE_EMOJI)
                } catch (e: MissingPermissionsException) {
                }
                true
            }.execute()
        } else {
            RequestBuffer.request {
                try {
                    msg.addReaction(Bot.CURRENCY_EMOJI)
                } catch (e: Exception) {
                }
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

            val url = Bot.getMessageMediaUrl(message, MediaType.IMAGE_AND_VIDEO)?: return null
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

    fun addCurrency(user: IUser, amount: Int) {
        currency[user.longID] = currency.getOrDefault(user.longID, 0) + amount
    }

    fun getCurrency(user: IUser): Int {
        return currency.getOrDefault(user.longID, 0)
    }

    fun getAverageVote(): Int {
        if (posts == 0) return 0
        return (votes.toFloat() / posts.toFloat()).roundToInt()
    }

    fun getZeroRank(): Int {
        for (value in leaderboard.valueMap.values) {
            if (value.value <= 0)
                return value.key
        }
        return leaderboard.valueMap.size
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

    fun sortHashMapByValues(passedMap: java.util.HashMap<Long, Int>): LinkedHashMap<Long, Int> {
        val mapKeys = ArrayList(passedMap.keys)
        val mapValues = ArrayList(passedMap.values)
        mapValues.sort()
        mapKeys.sort()

        val sortedMap = LinkedHashMap<Long, Int>()

        val valueIt = mapValues.iterator()
        while (valueIt.hasNext()) {
            val `val` = valueIt.next()
            val keyIt = mapKeys.iterator()

            while (keyIt.hasNext()) {
                val key = keyIt.next()
                val comp1 = passedMap[key]

                if (comp1 == `val`) {
                    keyIt.remove()
                    sortedMap[key] = `val`
                    break
                }
            }
        }
        return sortedMap
    }

}