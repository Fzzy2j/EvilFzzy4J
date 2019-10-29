package me.fzzy.evilfzzy4j

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import me.fzzy.evilfzzy4j.carbonationwars.battleship.BattleshipChannel
import me.fzzy.evilfzzy4j.carbonationwars.minesweeper.MinesweeperChannel
import me.fzzy.evilfzzy4j.command.Command
import me.fzzy.evilfzzy4j.command.admin.Override
import me.fzzy.evilfzzy4j.command.economy.LeaderboardCommand
import me.fzzy.evilfzzy4j.command.help.Help
import me.fzzy.evilfzzy4j.command.help.Invite
import me.fzzy.evilfzzy4j.command.help.Picturetypes
import me.fzzy.evilfzzy4j.command.image.*
import me.fzzy.evilfzzy4j.command.voice.Play
import me.fzzy.evilfzzy4j.command.voice.Tts
import me.fzzy.evilfzzy4j.util.ImageHelper
import me.fzzy.evilfzzy4j.voice.FzzyPlayer
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.*
import org.im4java.process.ProcessStarter
import org.json.JSONObject
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
    val SAD_EMOJIS = "\uD83D\uDE22"
    val HAPPY_EMOJIS = "\uD83D\uDE04"
    val SURPRISED_EMOJIS = "\uD83D\uDE2F"
}

object Bot {
    lateinit var client: JDA

    val logger = Loggers.getLogger(Bot::class.java)

    val gson = Gson()

    val scheduler = Schedulers.elastic()

    val playerManager = DefaultAudioPlayerManager()
    private val audioManagers = hashMapOf<Long, FzzyPlayer>()

    //var speechApiToken: String? = null

    lateinit var data: BotData

    lateinit var currencyEmoji: Emote
    lateinit var sadEmoji: Emote
    lateinit var happyEmoji: Emote
    lateinit var surprisedEmoji: Emote

    val URL_PATTERN: Pattern = Pattern.compile("(?:^|[\\W])((ht|f)tp(s?):\\/\\/)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)")

    val random = Random()

    val DATA_FILE = File("data")
    val TTS_AUTH_FILE = File("google-tts.json")

    fun getGuildAudioPlayer(guild: Guild): FzzyPlayer {
        var manager = audioManagers[guild.idLong]
        if (manager == null) {
            manager = FzzyPlayer(guild)
            audioManagers[guild.idLong] = manager
        }
        return manager
    }

    fun getRecentImage(channel: MessageChannel): File? {
        if (channel is TextChannel) return getRecentImage(channel)
        for (msg in channel.getHistoryBefore(channel.latestMessageId, 10).complete().retrievedHistory) {
            val media = getMessageMedia(msg)
            if (media != null) {
                return ImageHelper.downloadTempFile(media)
            }
        }
        return null
    }

    fun getRecentImage(channel: TextChannel): File? {
        for (msg in channel.getHistoryBefore(channel.latestMessageId, 10).complete().retrievedHistory) {
            val media = getMessageMedia(msg)
            if (media != null) {
                return ImageHelper.downloadTempFile(media)
            }
        }
        return ImageHelper.createTempFile(Repost.getImageRepost(channel.guild))
    }

    fun getFirstUrl(string: String): URL? {
        val matcher = URL_PATTERN.matcher(string)

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

    fun getMessageMedia(message: Message): URL? {
        if (message.attachments.size == 1) {
            val attach = message.attachments.single()
            if (attach.isImage) return URL(attach.url)
        }
        val url = getFirstUrl(message.contentRaw)
        return if (url != null) if (isMedia(url)) url else null else null
    }
}

fun main(args: Array<String>) {
    Bot.DATA_FILE.mkdirs()

    //Bot.playerManager.registerSourceManager(LocalAudioSourceManager())
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

    val cacheFile = File("cache")
    if (cacheFile.exists()) {
        for (file in cacheFile.listFiles()!!) {
            file.deleteRecursively()
        }
    } else cacheFile.mkdirs()

    Bot.logger.info("Logging in.")
    Bot.client = JDABuilder()
            .setActivity(Activity.listening("the rain"))
            .setToken(Bot.data.DISCORD_TOKEN)
            .addEventListeners(Command, ReactionHandler)
            .build()

    Bot.logger.info("Initializing game channels.")
    Bot.client.addEventListener(BattleshipChannel(608427341240205348L))
    Bot.client.addEventListener(MinesweeperChannel(608780391083671586L))

    Bot.client.awaitReady()

    Bot.currencyEmoji = Bot.client.getEmoteById(571593175907434516L)!!
    Bot.sadEmoji = Bot.client.getEmoteById(627647150620278805L)!!
    Bot.happyEmoji = Bot.client.getEmoteById(627636612288741406L)!!
    Bot.surprisedEmoji = Bot.client.getEmoteById(593542628709105896L)!!
    Bot.logger.info("Currency emoji set to: ${Bot.currencyEmoji}")
    Bot.logger.info("Sad emojis set to: ${Bot.sadEmoji}")
    Bot.logger.info("Happy emojis set to: ${Bot.happyEmoji}")
    Bot.logger.info("Surprised emojis set to: ${Bot.surprisedEmoji}")

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

    Bot.logger.info("Starting auto-saver.")
    Bot.scheduler.schedulePeriodically({
        FzzyGuild.saveAll()
    }, 10, 60, TimeUnit.SECONDS)

    Bot.logger.info("EvilFzzy v${Bot::class.java.`package`.implementationVersion} online.")

    //ReactionHandler.registerEvents(Bot.client.eventDispatcher)

    /*Bot.client.eventDispatcher.on(MessageCreateEvent::class.java)
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
                                                .onErrorResume { Mono.empty() }
                                                .doOnError {
                                                    Bot.logger.error(it.message!!)
                                                }
                                    }
                                    .next()
                        }
            }
            .subscribeOn(Bot.scheduler)
            .subscribe()*/

    /*Bot.client.eventDispatcher.on(MessageCreateEvent::class.java).doOnError {
        Bot.logger.error(it.message!!)
    }.subscribe { event ->

        if (!event.message.author.get().isBot) {
            if (event.guildId.isPresent) {
                val guild = FzzyGuild.getGuild(event.guildId.get())
                val msg = event.message.content
                if ((msg.isPresent && Bot.URL_PATTERN.matcher(event.message.content.get()).find()) || event.message.attachments.size > 0) {
                    guild.allowVotes(event.message)
                }
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
    }*/

    /*fun parseEmoji(s: String): ReactionEmoji {
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
    }*/
}


