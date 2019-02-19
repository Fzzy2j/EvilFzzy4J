package me.fzzy.robofzzy4j

import me.fzzy.robofzzy4j.listeners.VoteListener
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.RequestBuilder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.regex.Pattern
import kotlin.math.roundToInt

class Guild private constructor(private var guildId: Long) {

    companion object {
        private val guilds = arrayListOf<Guild>()

        fun getGuild(guildId: Long): Guild {
            for (guild in guilds) {
                if (guild.longId == guildId)
                    return guild
            }
            val guild = Guild(guildId)
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

    init {
        load()
    }

    val longId: Long = this.guildId
    lateinit var leaderboard: Leaderboard

    private var posts = 0
    private var votes = 0

    private var changes: Leaderboard.LeaderboardChange? = null

    fun handleChanges(change: Leaderboard.LeaderboardChange, channel: IChannel) {
        if (change.positions.isEmpty())
            return

        if (changes == null) changes = change

        val messages = channel.getMessageHistory(5)
        var existingMessage: IMessage? = null

        for (history in messages) {
            if (history.content.startsWith("```diff\n--- Leaderboard Changes\n") && history.author.longID == RoboFzzy.cli.ourUser.longID) {
                existingMessage = history
                break
            }
        }
        if (existingMessage == null) changes = change else changes?.add(change)

        var message = "```diff\n--- Leaderboard Changes\n"
        for ((id, amt) in Funcs.sortHashMapByValues(changes!!.positions)) {
            if (amt == 0) continue
            val name = RoboFzzy.cli.getUserByID(id).getDisplayName(getDiscordGuild())
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
            RequestBuffer.request { channel.sendMessage(message) }
    }

    fun addPoint(message: IMessage, user: IUser, channel: IChannel) {
        val score = leaderboard.getOrDefault(user.longID, 0)
        if (!user.isBot) handleChanges(leaderboard.setValue(user.longID, score + 1), channel)
        votes++

        if (VoteListener.getVotes(message) > getAverageVote()) {
            if (!RoboFzzy.savedMemesIds.contains(message.longID)) {
                RoboFzzy.savedMemesIds.add(message.longID)
                if (message.attachments.size == 0) {
                    if (!(message.content.toLowerCase().endsWith(".png")
                                    || message.content.toLowerCase().endsWith(".jpg")
                                    || message.content.toLowerCase().endsWith(".jpeg")
                                    || message.content.toLowerCase().endsWith(".gif")
                                    || message.content.toLowerCase().endsWith(".webm")))
                        return
                }

                val pattern = Pattern.compile("^(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
                lateinit var url: URL
                if (message.attachments.size > 0) {
                    url = URL(message.attachments[0].url)
                } else {
                    for (split in message.content.split(" ")) {
                        val matcher = pattern.matcher(split)
                        if (matcher.find()) {
                            var urlString = split.substring(matcher.start(1), matcher.end()).replace(".webp", ".png").replace("//gyazo.com", "//i.gyazo.com")
                            if (urlString.contains("i.gyazo.com") && !urlString.endsWith(".png")) {
                                urlString += ".png"
                            }
                            if (urlString.contains("i.gyazo.com") && !urlString.endsWith(".jpg")) {
                                urlString += ".jpq"
                            }
                            url = URL(urlString)
                            break
                        }
                    }
                }
                val fixedUrl = URL(url.toString().replace(".gifv", ".gif"))
                var suffix = "jpg"
                if (fixedUrl.toString().endsWith("webp") || fixedUrl.toString().endsWith("png"))
                    suffix = "png"
                if (fixedUrl.toString().endsWith("gif"))
                    suffix = "gif"
                if (fixedUrl.toString().endsWith("webm"))
                    suffix = "webm"
                if (fixedUrl.toString().endsWith("mp4"))
                    suffix = "mp4"

                File("memes", guildId.toString()).mkdirs()
                val fileName = "memes/$guildId/${System.currentTimeMillis()}.$suffix"
                try {
                    val openConnection = fixedUrl.openConnection()
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
            }
        }
    }

    fun subtractPoint(user: IUser, channel: IChannel) {
        val score = leaderboard.getOrDefault(user.longID, 0)
        if (!user.isBot) handleChanges(leaderboard.setValue(user.longID, score - 1), channel)
        votes--
    }

    fun allowVotes(msg: IMessage) {
        posts++
        votes++
        RequestBuilder(RoboFzzy.cli).shouldBufferRequests(true).doAction {
            try {
                msg.addReaction(RoboFzzy.UPVOTE_EMOJI)
            } catch (e: MissingPermissionsException) {
            }
            true
        }.andThen {
            try {
                msg.addReaction(RoboFzzy.DOWNVOTE_EMOJI)
            } catch (e: MissingPermissionsException) {
            }
            true
        }.execute()
    }

    fun getAverageVote(): Int {
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
        return RoboFzzy.cli.getGuildByID(longId)
    }

    fun save() {
        RoboFzzy.guildNode.getNode("id$guildId", "votes").value = null
        var i = 0
        for ((key, value) in leaderboard.valueMap) {
            RoboFzzy.guildNode.getNode("id$guildId", "votes", i, "id").value = key
            RoboFzzy.guildNode.getNode("id$guildId", "votes", i, "value").value = value.value
            i++
        }

        RoboFzzy.guildNode.getNode("id$guildId", "totalVotes").value = votes
        RoboFzzy.guildNode.getNode("id$guildId", "totalPosts").value = posts

        RoboFzzy.guildManager.save(RoboFzzy.guildNode)
    }

    fun load() {
        leaderboard = Leaderboard()
        for (node in RoboFzzy.guildNode.getNode("id$guildId", "votes").childrenList) {
            if (try {
                        !RoboFzzy.cli.getUserByID(node.getNode("id").long).isBot
                    } catch (e: Exception) {
                        true
                    })
                leaderboard.setValue(node.getNode("id").long, node.getNode("value").int)
        }
        votes = RoboFzzy.guildNode.getNode("id$guildId", "totalVotes").int
        posts = RoboFzzy.guildNode.getNode("id$guildId", "totalPosts").int
    }

}