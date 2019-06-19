package me.fzzy.robofzzy4j

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import discord4j.core.DiscordClient
import discord4j.core.DiscordClientBuilder
import discord4j.core.`object`.entity.Message
import discord4j.core.`object`.presence.Activity
import discord4j.core.`object`.presence.Presence
import discord4j.core.`object`.reaction.ReactionEmoji
import discord4j.core.event.domain.message.MessageCreateEvent
import me.fzzy.robofzzy4j.commands.*
import me.fzzy.robofzzy4j.commands.help.Help
import me.fzzy.robofzzy4j.commands.help.Invite
import me.fzzy.robofzzy4j.commands.help.Picturetypes
import me.fzzy.robofzzy4j.util.MediaType
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
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class BotData {
    val DISCORD_TOKEN = ""
    val BOT_PREFIX = "-"
    val DEFAULT_TEMP_MESSAGE_DURATION: Long = 15
    val IMAGE_MAGICK_DIRECTORY = "C:${File.separator}Program Files${File.separator}ImageMagick-7.0.8-Q16"
    val CURRENCY_EMOJI = "❤"
    val SAD_EMOJI = "\uD83D\uDE22"
    val HAPPY_EMOJI = "\uD83D\uDE04"
    val SURPRISED_EMOJI = "\uD83D\uDE2F"
    val COMPLACENT_EMOJI = "\uD83D\uDE0B"
}

object Bot {
    lateinit var client: DiscordClient

    val logger = Loggers.getLogger(Bot::class.java)

    val gson = Gson()

    val scheduler = Schedulers.elastic()

    val playerManager = DefaultAudioPlayerManager()

    //var speechApiToken: String? = null

    lateinit var data: BotData

    lateinit var CURRENCY_EMOJI: ReactionEmoji
    lateinit var SAD_EMOJI: ReactionEmoji
    lateinit var HAPPY_EMOJI: ReactionEmoji
    lateinit var SURPRISED_EMOJI: ReactionEmoji
    lateinit var COMPLACENT_EMOJI: ReactionEmoji

    val URL_PATTERN: Pattern = Pattern.compile("(?:^|[\\W])((ht|f)tp(s?):\\/\\/)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)")

    // Threads
    //var azureAuth: Authentication? = null

    val random = Random()

    val DATA_FILE = File("data")
    val TTS_AUTH_FILE = File("google-tts.json")

    fun getRecentImage(message: Message): Mono<URL?> {
        return message.channel
                .flatMap { channel ->
                    channel.getMessagesBefore(message.id)
                            .take(10)
                            .takeUntil { isMedia(URL(it.content.get())) }
                            .flatMap {
                                Mono.just(URL(it.content.get()))
                            }.next()
                }
    }

    fun isMedia(url: URL): Boolean {
        HttpURLConnection.setFollowRedirects(false)
        val con = url.openConnection() as HttpURLConnection
        con.requestMethod = "HEAD"
        logger.info(con.responseMessage)
        return con.responseCode == HttpURLConnection.HTTP_OK
    }

    fun getMessageMediaUrl(message: Message, type: MediaType): Mono<URL> {
        var url: URL? = null
        if (message.attachments.size == 1) {
            url = URL(message.attachments.single().url)
        } else {
            if (!message.content.isPresent) return Mono.empty()
            for (split in message.content.get().split(" ")) {
                val matcher = Bot.URL_PATTERN.matcher(split)
                if (matcher.find()) {
                    var urlString = split.toLowerCase().substring(matcher.start(1), matcher.end())
                            .replace(".webp", ".png")
                            .replace(".gifv", ".gif")
                            .replace("//gyazo.com", "//i.gyazo.com")
                    if (urlString.contains("i.gyazo.com") && !urlString.endsWith(".png")) {
                        urlString += ".png"
                    }
                    for (t in type.formats) {
                        if (urlString.endsWith(".$t")) {
                            url = URL(urlString)
                            break
                        }
                    }
                }
            }
        }
        return if (url != null) Mono.just(url)
        else Mono.empty()
    }
}

fun main(args: Array<String>) {
    Bot.DATA_FILE.mkdirs()

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
        System.exit(0)
    }

    if (!Bot.TTS_AUTH_FILE.exists()) {
        Bot.logger.error("Could not find ${Bot.TTS_AUTH_FILE.name}, -tts will not work")
    }

    Bot.logger.info("Bot Starting.")

    ProcessStarter.setGlobalSearchPath(Bot.data.IMAGE_MAGICK_DIRECTORY)

    Bot.logger.info("Registering commands.")

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
        for (file in cacheFile.listFiles()) {
            file.deleteRecursively()
        }
    }

    Bot.logger.info("Starting auto-saver.")

    // Autosave
    Bot.scheduler.schedulePeriodically({
        FzzyGuild.saveAll()
    }, 10, 60, TimeUnit.SECONDS)

    Bot.logger.info("Registering events.")

    Bot.client = DiscordClientBuilder(Bot.data.DISCORD_TOKEN).build()

    Bot.client.eventDispatcher.on(MessageCreateEvent::class.java)
            .flatMap { event ->
                Mono.justOrEmpty(event.message.content)
                        .flatMap { content ->
                            Flux.fromIterable(Command.commands.entries)
                                    .filter { entry -> content.startsWith("${Bot.data.BOT_PREFIX}${entry.key}") }
                                    .flatMap { entry ->
                                        entry.value.handleCommand(event)
                                                .flatMap { result ->
                                                    if ((!entry.value.votes || !result.isSuccess()) && event.message.attachments.count() == 0)
                                                        event.message.delete()
                                                    Mono.just(result)
                                                }
                                                .filter { !it.isSuccess() }
                                                .flatMap { result ->
                                                    event.message.channel.flatMap {
                                                        MessageScheduler.sendTempMessage(it, result.getFailMessage(), Bot.data.DEFAULT_TEMP_MESSAGE_DURATION, TimeUnit.SECONDS)
                                                    }
                                                }
                                    }
                                    .next()
                        }
            }
            .subscribe()

    Bot.client.eventDispatcher.on(MessageCreateEvent::class.java).subscribe { event ->

        val respongeMsgs = listOf(
                "no problem %name%",
                "np %name%",
                ":D",
                ":P"
        )

        fun mentionsByName(msg: Message): Boolean {
            val check = msg.content.get().toLowerCase()
            val self = msg.guild.block()!!.getMemberById(Bot.client.selfId.get()).block()!!
            val checkAgainst = "${self.username} ${self.nickname}"
            for (realCheck in check.split(" ")) {
                if (check.contains("thank") && (checkAgainst.toLowerCase().replace(" ", "").contains(realCheck) || msg.userMentions.collectList().block()!!.contains(self)))
                    return true
            }
            return false
        }
        /*if (mentionsByName(event.message)) {
            for (msg in event.channel.getMessageHistory(7)) {
                if (msg.author.longID == Bot.client.ourUser.longID) {
                    RequestBuffer.request {
                        MessageScheduler.sendTempMessage(Bot.data.DEFAULT_TEMP_MESSAGE_DURATION, event.channel, respongeMsgs[Bot.random.nextInt(respongeMsgs.size)].replace("%name%", event.author.getDisplayName(event.guild).toLowerCase()))
                    }
                    break
                }
            }
        }
        if (!event.author.isBot) {
            val guild = FzzyGuild.getGuild(event.guild.longID)
            val m: Matcher = Bot.URL_PATTERN.matcher(event.message.content)
            if (m.find() || event.message.attachments.size > 0) {
                guild.allowVotes(event.message)
            }
        }*/
    }

    Bot.logger.info("RoboFzzy v${Bot::class.java.`package`.implementationVersion} online.")
    Bot.client.updatePresence(Presence.online(Activity.listening("the rain ${Bot.data.BOT_PREFIX}help")))

    fun parseEmoji(s: String): ReactionEmoji {
        return try {
            val emojiSplit = s.substring(1, s.length - 1).split(":")
            ReactionEmoji.of(emojiSplit[2].toLong(), emojiSplit[1], emojiSplit[0].isNotBlank())
        } catch (e: Exception) {
            ReactionEmoji.unicode(s)
        }
    }

    Bot.CURRENCY_EMOJI = parseEmoji(Bot.data.CURRENCY_EMOJI)
    Bot.SAD_EMOJI = parseEmoji(Bot.data.SAD_EMOJI)
    Bot.HAPPY_EMOJI = parseEmoji(Bot.data.HAPPY_EMOJI)
    Bot.SURPRISED_EMOJI = parseEmoji(Bot.data.SURPRISED_EMOJI)
    Bot.COMPLACENT_EMOJI = parseEmoji(Bot.data.COMPLACENT_EMOJI)
    Bot.logger.info("Currency emoji set to: ${Bot.CURRENCY_EMOJI}")
    Bot.logger.info("Sad emoji set to: ${Bot.SAD_EMOJI}")
    Bot.logger.info("Happy emoji set to: ${Bot.HAPPY_EMOJI}")
    Bot.logger.info("Surprised emoji set to: ${Bot.SURPRISED_EMOJI}")
    Bot.logger.info("Complacent emoji set to: ${Bot.COMPLACENT_EMOJI}")

    Bot.logger.info("Logging in.")

    Bot.client.login()
}


