package me.fzzy.robofzzy4j.commands

import com.vdurmont.emoji.EmojiManager
import me.fzzy.robofzzy4j.Command
import me.fzzy.robofzzy4j.CommandResult
import me.fzzy.robofzzy4j.Guild
import me.fzzy.robofzzy4j.Bot
import me.fzzy.robofzzy4j.listeners.VoteListener
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionRemoveEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IReaction
import sx.blah.discord.handle.obj.IUser
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.RequestBuilder
import java.util.regex.Pattern
import kotlin.math.roundToInt

object Vote : Command {

    override val cooldownCategory = "vote"
    override val cooldownMillis: Long = 1000 * 60 * 5
    override val description: String =
            "Puts a message in the chat that allows users to vote on something, if no options are provided it defaults to Yeah and No" +
            "\nExample: -vote mem on monday?|totally|maybe|absolutely not|toast"
    override val votes: Boolean = false
    override val usageText: String = "-vote <message|options>"
    override val allowDM: Boolean = false

    private const val BEGINNING = "```diff\n- Vote - "

    override fun runCommand(event: MessageReceivedEvent, args: List<String>): CommandResult {
        if (args.isEmpty()) return CommandResult.fail("i should really put a message on that or give it some options $usageText")
        if (args.joinToString(" ").contains("|")) {
            val options = args.joinToString(" ").split("|")
            sendVoteMessage(event.channel, options[0], options.subList(1, options.size))
            return CommandResult.success()
        }
        sendVoteMessage(event.channel, args.joinToString(" "), listOf("Yeah", "No"))
        return CommandResult.success()
    }

    fun getAttendanceMessage(channel: IChannel): IMessage? {
        for (msg in channel.getMessageHistory(20)) {
            if (msg.content.contains(BEGINNING) && msg.author.longID == Bot.client.ourUser.longID) {
                return msg
            }
        }
        return null
    }

    fun extractMessage(msg: String): String {
        return msg.substring(BEGINNING.length, msg.indexOf("\n\n1 - \""))
    }

    fun extractOptions(msg: String): ArrayList<String> {
        val p = Pattern.compile("\"([^\"]*)\"")
        val m = p.matcher(msg)
        val list = arrayListOf<String>()
        while (m.find()) {
            list.add(m.group(1))
        }
        return list
    }

    fun updateVoteMessage(msg: IMessage, reactions: List<IReaction>, message: String = extractMessage(msg.content)) {
        val ones = VoteMessage.getUsersReacted(reactions, 1)
        val twos = VoteMessage.getUsersReacted(reactions, 2)
        val threes = VoteMessage.getUsersReacted(reactions, 3)
        val fours = VoteMessage.getUsersReacted(reactions, 4)
        val fives = VoteMessage.getUsersReacted(reactions, 5)
        val sixes = VoteMessage.getUsersReacted(reactions, 6)
        val sevens = VoteMessage.getUsersReacted(reactions, 7)
        val eights = VoteMessage.getUsersReacted(reactions, 8)
        val nines = VoteMessage.getUsersReacted(reactions, 9)
        val tens = VoteMessage.getUsersReacted(reactions, 10)
        val options = extractOptions(msg.content)
        RequestBuffer.request { msg.edit(generateVoteMessage(VoteMessage(message, options, ones, twos, threes, fours, fives, sixes, sevens, eights, nines, tens))) }
    }

    fun sendVoteMessage(channel: IChannel, message: String, options: List<String>) {
        RequestBuffer.request { addNumberReactions(channel.sendMessage(generateVoteMessage(VoteMessage(message, ArrayList(options)))), options.size) }
    }

    fun generateVoteMessage(message: VoteMessage): String {
        var t = "$BEGINNING${message.message}"
        var i = 0
        var total = 0
        for (s in 1..Math.max(message.options.size, 10)) {
            total += message.getListByNumber(s).size
        }
        message.options.forEach { option ->
            i++
            if (i <= 10) {
                t += "\n\n$i - \"$option\"\n"
                t += if (total == 0)
                    "+ 0%"
                else
                    "+ ${(message.getListByNumber(i).size / total.toFloat() * 100).roundToInt()}%"
            }
        }
        t += "```"
        return t
    }

    fun addNumberReactions(message: IMessage, amount: Int) {

        val request = RequestBuilder(Bot.client).shouldBufferRequests(true).doAction {
            message.addReaction(EmojiManager.getForAlias(numNames[1]))
            true
        }

        for (i in 2..amount) {
            request.andThen {
                message.addReaction(EmojiManager.getForAlias(numNames[i]))
                true
            }
        }

        request.execute()
    }

    @EventSubscriber
    fun onReactionAdd(event: ReactionAddEvent) {
        if (event.message.author.longID == Bot.client.ourUser.longID && !event.user.isBot) {
            if (getAttendanceMessage(event.channel) != null) {

                // pass reactions so that we only have to make one request to the api, speeds up voting a lot
                val reactions = event.message.reactions
                //removeExistingReactionsExceptOne(event.user, event.message, reactions, event.reaction.emoji.name)
                updateVoteMessage(event.message, reactions)
            }
        }
    }

    @EventSubscriber
    fun onReactionRemove(event: ReactionRemoveEvent) {
        if (event.message.author.longID == Bot.client.ourUser.longID && !event.user.isBot) {
            if (getAttendanceMessage(event.channel) != null) {

                // pass reactions so that we only have to make one request to the api, speeds up voting a lot
                val reactions = event.message.reactions
                updateVoteMessage(event.message, reactions)
            }
        }
    }

    fun removeExistingReactionsExceptOne(user: IUser, message: IMessage, reactions: List<IReaction>, exception: String) {
        for (reaction in reactions) {
            if (reaction.getUserReacted(user)) {
                if (reaction.emoji.name != exception)
                    message.removeReaction(user, reaction)
            }
        }
    }

    private val numNames = arrayOf(
            "",
            "one",
            "two",
            "three",
            "four",
            "five",
            "six",
            "seven",
            "eight",
            "nine",
            "keycap_ten"
    )

    class VoteMessage constructor(
            val message: String = "",
            val options: ArrayList<String> = arrayListOf(),
            val ones: List<IUser> = listOf(),
            val twos: List<IUser> = listOf(),
            val threes: List<IUser> = listOf(),
            val fours: List<IUser> = listOf(),
            val fives: List<IUser> = listOf(),
            val sixes: List<IUser> = listOf(),
            val sevens: List<IUser> = listOf(),
            val eights: List<IUser> = listOf(),
            val nines: List<IUser> = listOf(),
            val tens: List<IUser> = listOf()
    ) {

        companion object {
            fun getUsersReacted(msg: List<IReaction>, i: Int): List<IUser> {
                for (reaction in msg) {
                    if (reaction.emoji.name == EmojiManager.getForAlias(numNames[i]).unicode) {
                        return reaction.users.filter { user ->
                            if (user != null) !user.isBot else false
                        }
                    }
                }
                return listOf()
            }
        }

        fun getListByNumber(i: Int): List<IUser> {
            when (i) {
                1 -> return ones
                2 -> return twos
                3 -> return threes
                4 -> return fours
                5 -> return fives
                6 -> return sixes
                7 -> return sevens
                8 -> return eights
                9 -> return nines
                10 -> return tens
            }
            return listOf()
        }
    }
}