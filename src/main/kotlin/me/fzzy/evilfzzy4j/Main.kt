package me.fzzy.evilfzzy4j

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.local.LocalAudioSourceManager
import discord4j.core.DiscordClient
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.entity.MessageChannel
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Presence
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.`object`.util.Snowflake
import discord4j.core.event.domain.message.MessageCreateEvent
import me.fzzy.evilfzzy4j.carbonationwars.battleship.BattleshipChannel
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.admin.Override
import me.fzzy.evilfzzy4j.command.economy.LeaderboardCommand
import me.fzzy.evilfzzy4j.command.help.Help
import me.fzzy.evilfzzy4j.command.help.Invite
import me.fzzy.evilfzzy4j.command.help.Picturetypes
import me.fzzy.evilfzzy4j.command.image.*
import me.fzzy.evilfzzy4j.command.voice.Play
import me.fzzy.evilfzzy4j.command.voice.Tts
import org.im4java.process.ProcessStarter
import org.json.JSONObject
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.util.Loggers
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern
import javax.imageio.ImageIO
import kotlin.system.exitProcess

class BotData {
    val DISCORD_TOKEN = ""
    val BOT_PREFIX = "-"
    val DEFAULT_TEMP_MESSAGE_DURATION: Long = 15
    val IMAGE_MAGICK_DIRECTORY = "C:${File.separator}Program Files${File.separator}ImageMagick-7.0.8-Q16"
    val CURRENCY_EMOJI = "❤"
    val SAD_EMOJIS = arrayListOf("\uD83D\uDE22")
    val HAPPY_EMOJIS = arrayListOf("\uD83D\uDE04")
    val SURPRISED_EMOJIS = arrayListOf("\uD83D\uDE2F")
}

object Bot {
    lateinit var client: DiscordClient

    val logger = Loggers.getLogger(Bot::class.java)

    val gson = Gson()

    val scheduler = Schedulers.elastic()

    val playerManager = DefaultAudioPlayerManager()

    lateinit var ranch: BattleshipChannel

    //var speechApiToken: String? = null

    lateinit var data: BotData

    lateinit var currencyEmoji: ReactionEmoji
    lateinit var sadEmojis: List<ReactionEmoji>
    lateinit var happyEmojis: List<ReactionEmoji>
    lateinit var surprisedEmojis: List<ReactionEmoji>

    val URL_PATTERN: Pattern = Pattern.compile("(?:^|[\\W])((ht|f)tp(s?):\\/\\/)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)")

    val random = Random()

    val DATA_FILE = File("data")
    val TTS_AUTH_FILE = File("google-tts.json")

    fun getRecentImage(channel: MessageChannel): Mono<URL> {
        return channel.getMessagesBefore(channel.lastMessageId.get())
                .take(10)
                .filter { isMessageMedia(it) }
                .flatMap {
                    val media = getMessageMedia(it)
                    if (media != null)
                        Mono.just(media)
                    else
                        Mono.empty()
                }.next()
    }

    fun getFirstUrl(string: String): URL? {
        val matcher = Bot.URL_PATTERN.matcher(string)

        if (matcher.find()) {
            val url = string.substring(matcher.start(1), matcher.end())
            return URL(url)
        }
        return null
    }

    fun isMedia(url: URL?): Boolean {
        return try {
            val image = ImageIO.read(url)
            image != null
        } catch (e: Exception) {
            false
        }
    }

    fun isMessageMedia(message: Message): Boolean {
        for (a in message.attachments) {
            if (a.width.isPresent) return true
        }
        val content = message.content
        if (content.isPresent) {
            val url = getFirstUrl(content.get())
            if (url != null) return isMedia(url)
        }
        return false
    }

    fun getMessageMedia(message: Message): URL? {
        if (message.attachments.size == 1) {
            val attach = message.attachments.single()
            if (attach.width.isPresent) return URL(attach.url)
        }
        val content = message.content
        if (content.isPresent) {
            val url = getFirstUrl(content.get())
            if (url != null) return url
        }
        return null
    }

    fun happyEmoji(): String {
        val e = happyEmojis[Bot.random.nextInt(happyEmojis.count())]
        return toUsable(e)
    }

    fun sadEmoji(): String {
        val e = sadEmojis[Bot.random.nextInt(sadEmojis.count())]
        return toUsable(e)
    }

    fun surprisedEmoji(): String {
        val e = surprisedEmojis[Bot.random.nextInt(surprisedEmojis.count())]
        return toUsable(e)
    }

    fun toUsable(emoji: ReactionEmoji): String {
        return if (emoji is ReactionEmoji.Custom) {
            val a = if (emoji.isAnimated) "a" else ""
            "<$a:${emoji.name}:${emoji.id.asString()}>"
        } else {
            emoji.asUnicodeEmoji().get().raw
        }
    }
}

fun main(args: Array<String>) {
    Bot.DATA_FILE.mkdirs()

    Bot.playerManager.registerSourceManager(LocalAudioSourceManager())
    AudioSourceManagers.registerLocalSource(Bot.playerManager)

    Bot.logger.info("Loading data.")
    val dataFile = File("config.json")
    if (dataFile.exists()) Bot.data = Bot.gson.fromJson(JsonReader(InputStreamReader(dataFile.inputStream())), BotData::class.java)
    else {
        Bot.data = BotData()
        val bufferWriter = BufferedWriter(FileWriter(dataFile.absoluteFile, false))
        val save = JSONObject(Bot.gson.toJson(Bot.data))
        bufferWriter.write(save.toString(2))
        bufferWriter.close()
    }

    if (Bot.data.DISCORD_TOKEN.isBlank()) {
        Bot.logger.error("You must set your discord token in config.json so that the bot can log in.")
        exitProcess(0)
    }

    if (!Bot.TTS_AUTH_FILE.exists()) {
        Bot.logger.error("Could not find ${Bot.TTS_AUTH_FILE.name}, -tts will not work")
    }

    Bot.logger.info("Bot Starting.")

    ProcessStarter.setGlobalSearchPath(Bot.data.IMAGE_MAGICK_DIRECTORY)

    Bot.logger.info("Registering command.")

    Command.registerCommand("fzzy", Fzzy)
    Command.registerCommand("picture", Picture)
    Command.registerCommand("deepfry", Deepfry)
    Command.registerCommand("mc", Mc)
    Command.registerCommand("explode", Explode)
    Command.registerCommand("meme", Meme)
    Command.registerCommand("play", Play)
    Command.registerCommand("tts", Tts)

    Command.registerCommand("repost", Repost)

    Command.registerCommand("leaderboard", LeaderboardCommand)

    Command.registerCommand("help", Help)
    Command.registerCommand("invite", Invite)
    Command.registerCommand("picturetypes", Picturetypes)
    Command.registerCommand("override", Override)

    val cacheFile = File("cache")
    if (cacheFile.exists()) {
        for (file in cacheFile.listFiles()!!) {
            file.deleteRecursively()
        }
    }

    Bot.logger.info("Starting auto-saver.")

    // Autosave
    Bot.scheduler.schedulePeriodically({
        FzzyGuild.saveAll()
        Bot.client.updatePresence(Presence.online(Activity.listening("the rain ${Bot.data.BOT_PREFIX}help"))).block()
    }, 10, 60, TimeUnit.SECONDS)

    Bot.logger.info("Registering events.")

    Bot.client = DiscordClientBuilder(Bot.data.DISCORD_TOKEN).build()

    ReactionHandler.registerEvents(Bot.client.eventDispatcher)

    Bot.client.eventDispatcher.on(MessageCreateEvent::class.java)
            .flatMap { event ->
                Mono.justOrEmpty(event.message.content)
                        .flatMap { content ->
                            Flux.fromIterable(Command.commands.entries)
                                    .filter { entry -> content.startsWith("${Bot.data.BOT_PREFIX}${entry.key}") }
                                    .flatMap { entry ->
                                        entry.value.handleCommand(event)
                                                .flatMap { result ->
                                                    Mono.just(result)
                                                }
                                                .filter { it.getMessage() != null }
                                                .flatMap { result ->
                                                    event.message.channel.flatMap {
                                                        MessageScheduler.sendTempMessage(it, result.getMessage()!!, Bot.data.DEFAULT_TEMP_MESSAGE_DURATION)
                                                    }
                                                }
                                                .doOnError {
                                                    Bot.logger.error(it.message!!)
                                                }
                                    }
                                    .next()
                        }
            }
            .subscribeOn(Bot.scheduler)
            .subscribe()

    Bot.client.eventDispatcher.on(MessageCreateEvent::class.java).doOnError {
        Bot.logger.error(it.message!!)
    }.subscribe { event ->

        if (!event.message.author.get().isBot) {
            val guild = FzzyGuild.getGuild(event.guildId.get())
            val msg = event.message.content
            if ((msg.isPresent && Bot.URL_PATTERN.matcher(event.message.content.get()).find()) || event.message.attachments.size > 0) {
                guild.allowVotes(event.message)
            }
        }

        val respongeMsgs = listOf(
                "no problem %name%",
                "np %name%",
                ":P"
        )

        fun mentionsByName(msg: Message): Boolean {
            if (!msg.content.isPresent) return false
            val check = msg.content.get().toLowerCase()
            val self = msg.guild.block()!!.getMemberById(Bot.client.selfId.get()).block()!!
            val checkAgainst = "${self.username} ${self.nickname}"
            for (realCheck in check.split(" ")) {
                if (check.contains("thank") && (checkAgainst.toLowerCase().replace(" ", "").contains(realCheck) || msg.userMentions.collectList().block()!!.contains(self)))
                    return true
            }
            return false
        }
        if (mentionsByName(event.message)) {
            event.message.channel.subscribe { channel ->
                channel.getMessagesBefore(channel.lastMessageId.get()).take(5).subscribe({
                    Bot.logger.info(event.message.timestamp.minusMillis(it.timestamp.toEpochMilli()).epochSecond.toString())
                    if (it.author.get().id == Bot.client.selfId.get()) {
                        val msg = respongeMsgs[Bot.random.nextInt(respongeMsgs.size)].replace("%name%", event.member.get().displayName.toLowerCase())
                        event.message.channel.block()!!.createMessage(msg).block()
                        throw RuntimeException("Found recent message from bot")
                    }
                }, {})
            }
        }
    }


    val ranchFile = File("ranch.json")
    if (ranchFile.exists()) Bot.ranch = Bot.gson.fromJson(JsonReader(InputStreamReader(ranchFile.inputStream())), BattleshipChannel::class.java)
    else Bot.ranch = BattleshipChannel(Snowflake.of(608427341240205348))

    Bot.logger.info("EvilFzzy v${Bot::class.java.`package`.implementationVersion} online.")
    Bot.client.updatePresence(Presence.online(Activity.listening("the rain ${Bot.data.BOT_PREFIX}help"))).block()

    fun parseEmoji(s: String): ReactionEmoji {
        return try {
            val emojiSplit = s.substring(1, s.length - 1).split(":")
            ReactionEmoji.of(emojiSplit[2].toLong(), emojiSplit[1], emojiSplit[0].isNotBlank())
        } catch (e: Exception) {
            ReactionEmoji.unicode(s)
        }
    }

    fun parseEmojis(list: List<String>): List<ReactionEmoji> {
        val l = arrayListOf<ReactionEmoji>()
        for (s in list) {
            l.add(parseEmoji(s))
        }
        return l
    }

    Bot.currencyEmoji = parseEmoji(Bot.data.CURRENCY_EMOJI)
    Bot.sadEmojis = parseEmojis(Bot.data.SAD_EMOJIS)
    Bot.happyEmojis = parseEmojis(Bot.data.HAPPY_EMOJIS)
    Bot.surprisedEmojis = parseEmojis(Bot.data.SURPRISED_EMOJIS)
    Bot.logger.info("Currency emoji set to: ${Bot.currencyEmoji}")
    Bot.logger.info("Sad emojis set to: ${Bot.sadEmojis}")
    Bot.logger.info("Happy emojis set to: ${Bot.happyEmojis}")
    Bot.logger.info("Surprised emojis set to: ${Bot.surprisedEmojis}")

    Bot.logger.info("Logging in.")

    Bot.client.login().block()
}


