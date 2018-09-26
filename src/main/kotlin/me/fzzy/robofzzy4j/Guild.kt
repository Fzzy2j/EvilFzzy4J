package me.fzzy.robofzzy4j

import me.fzzy.robofzzy4j.listeners.VoteListener
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.RequestBuilder

class Guild private constructor(private var guildId: Long) {

    companion object {
        private val guilds = arrayListOf<Guild>()

        fun getGuild(guildId: Long): Guild {
            for (guild in guilds) {
                if (guild.longId == guildId)
                    return guild
            }
            println("initializing new guild")
            val guild = Guild(guildId)
            guilds.add(guild)
            return guild
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

    fun addPoint(user: IUser, channel: IChannel) {
        val score = leaderboard.getOrDefault(user.longID, 0)
        val passed = leaderboard.setValue(user.longID, score + 1)
        votes++

        if (passed.isNotEmpty()) {
            var names = ""
            for (id in passed) {
                val name = cli.getUserByID(id).getDisplayName(getDiscordGuild())
                names += ", $name"
            }
            names = names.substring(2)

            channel.sendMessage("${user.getDisplayName(getDiscordGuild())} has passed $names on the leaderboard, their cooldown is now " + User.getUser(user).getCooldownModifier(this))
        }
    }

    fun subtractPoint(user: IUser) {
        val score = leaderboard.getOrDefault(user.longID, 0)
        leaderboard.setValue(user.longID, score - 1)
        votes--
    }

    fun allowVotes(msg: IMessage) {
        posts++
        votes++
        RequestBuilder(cli).shouldBufferRequests(true).doAction {
            msg.addReaction(ReactionEmoji.of("upvote", VoteListener.UPVOTE_ID))
            true
        }.andThen {
            msg.addReaction(ReactionEmoji.of("downvote", VoteListener.DOWNVOTE_ID))
            true
        }.execute()
    }

    fun getAverageVote(): Int {
        return (posts.toFloat() / votes.toFloat()).toInt()
    }

    fun getZeroRank(): Int {
        for (value in leaderboard.valueMap.values) {
            if (value.value <= 0)
                return value.key
        }
        return leaderboard.valueMap.size
    }

    fun getDiscordGuild(): IGuild {
        return cli.getGuildByID(longId)
    }

    fun save() {
        if (leaderboard.valueMap.size > 0) {
            var i = 0
            for ((key, value) in leaderboard.valueMap) {
                guildNode.getNode(guildId.toString(), "votes", i, "id").value = key
                guildNode.getNode(guildId.toString(), "votes", i, "value").value = value.value
                i++
            }

            guildNode.getNode(guildId.toString(), "totalVotes").value = votes
            guildNode.getNode(guildId.toString(), "totalPosts").value = posts

            guildManager.save(guildNode)
        }
    }

    fun load() {
        leaderboard = Leaderboard()
        for (node in guildNode.getNode(guildId.toString(), "votes").childrenList) {
            leaderboard.setValue(node.getNode("id").long, node.getNode("value").int)
        }
        votes = guildNode.getNode(guildId.toString(), "totalVotes").int
        posts = guildNode.getNode(guildId.toString(), "totalPosts").int
    }

}