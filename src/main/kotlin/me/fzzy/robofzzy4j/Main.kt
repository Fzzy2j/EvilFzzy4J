package me.fzzy.robofzzy4j

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
import me.fzzy.robofzzy4j.commands.*
import me.fzzy.robofzzy4j.commands.help.*
import me.fzzy.robofzzy4j.listeners.MessageListener
import me.fzzy.robofzzy4j.listeners.VoiceListener
import me.fzzy.robofzzy4j.listeners.VoteListener
import me.fzzy.robofzzy4j.thread.Authentication
import me.fzzy.robofzzy4j.thread.Scheduler
import org.im4java.process.ProcessStarter
import sx.blah.discord.Discord4J
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import java.io.File
import java.util.*
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.RequestBuffer
import java.io.BufferedWriter
import java.io.FileWriter
import java.io.InputStreamReader
import java.nio.file.Files
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern
import kotlin.collections.HashMap

class BotData {
    val BOT_PREFIX = "-"
    val DEFAULT_TEMP_MESSAGE_DURATION: Long = 15 * 1000
    val THREAD_COUNT = 4
    var leaderboardResetStamp = 0L
}

object Bot {
    lateinit var client: IDiscordClient

    val gson = Gson()

    var speechApiToken: String? = null

    var data = BotData()

    val UPVOTE_EMOJI = ReactionEmoji.of("upvote", 445376322353496064)!!
    val DOWNVOTE_EMOJI = ReactionEmoji.of("downvote", 445376330989830147)!!
    val CURRENCY_EMOJI = ReactionEmoji.of("diamond", 569090342897319936)!!

    val URL_PATTERN = Pattern.compile("(?:^|[\\W])((ht|f)tp(s?):\\/\\/)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)")

    // Threads
    var azureAuth: Authentication? = null

    val random = Random()

    val DATA_DIR: String = "data${File.separator}"

    lateinit var executor: ExecutorService

    @EventSubscriber
    fun onReady(event: ReadyEvent) {
        Discord4J.LOGGER.info("list of current guilds:")
        for (guild in event.client.guilds) {
            Discord4J.LOGGER.info(guild.name)
        }
        RequestBuffer.request { Bot.client.changePresence(StatusType.ONLINE, ActivityType.LISTENING, "the rain ${Bot.data.BOT_PREFIX}help") }
    }
}

fun main(args: Array<String>) {
    File(Bot.DATA_DIR).mkdirs()
    val keysFile = File("${Bot.DATA_DIR}keys.json")
    if (!keysFile.exists()) {
        Discord4J.LOGGER.warn("Generating new keys.json file... you must enter a discord token for the bot to function")
        Files.copy(Bot::class.java.classLoader.getResourceAsStream("${File.separator}keys.json"), keysFile.toPath())
        System.exit(0)
    }
    val type = object : TypeToken<HashMap<String, String>>() {}.type
    val keys: HashMap<String, String> = Bot.gson.fromJson(JsonReader(InputStreamReader(keysFile.inputStream())), type)

    Bot.speechApiToken = keys["speechApiToken"]

    Bot.executor = Executors.newFixedThreadPool(Bot.data.THREAD_COUNT)

    Discord4J.LOGGER.info("Bot Starting.")

    if (Bot.speechApiToken != null) Bot.azureAuth = Authentication(Bot.speechApiToken!!)
    ProcessStarter.setGlobalSearchPath("C:${File.separator}Program Files${File.separator}ImageMagick-7.0.8-Q16")

    Discord4J.LOGGER.info("Registering commands.")

    CommandHandler.registerCommand("fzzy", Fzzy)
    //CommandHandler.registerCommand("eyes", Eyes)
    CommandHandler.registerCommand("picture", Picture)
    CommandHandler.registerCommand("deepfry", Deepfry)
    CommandHandler.registerCommand("mc", Mc)
    CommandHandler.registerCommand("explode", Explode)
    CommandHandler.registerCommand("meme", Meme)
    CommandHandler.registerCommand("play", Play)
    CommandHandler.registerCommand("tts", Tts)
    CommandHandler.registerCommand("vote", Vote)

    CommandHandler.registerCommand("repost", Repost)

    CommandHandler.registerCommand("leaderboard", LeaderboardCommand)

    CommandHandler.registerCommand("help", Help)
    CommandHandler.registerCommand("invite", Invite)
    CommandHandler.registerCommand("sounds", Sounds)
    //CommandHandler.registerCommand("eyetypes", Eyetypes)
    CommandHandler.registerCommand("picturetypes", Picturetypes)
    CommandHandler.registerCommand("override", Override)

    Discord4J.LOGGER.info("Loading data.")
    val dataFile = File("${Bot.DATA_DIR}config.json")
    if (dataFile.exists()) Bot.data = Bot.gson.fromJson(JsonReader(InputStreamReader(dataFile.inputStream())), BotData::class.java)
    else {
        val bufferWriter = BufferedWriter(FileWriter(dataFile.absoluteFile, false))
        val save = Bot.gson.toJson(Bot.data)
        bufferWriter.write(save)
        bufferWriter.close()
    }

    val cacheFile = File("cache")
    if (cacheFile.exists()) {
        for (file in cacheFile.listFiles()) {
            file.deleteRecursively()
        }
    }

    Discord4J.LOGGER.info("Starting scheduler.")

    Scheduler.start()

    Discord4J.LOGGER.info("Starting auto-saver.")

    // Autosave
    Scheduler.Builder(60).doAction {
        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (day == Calendar.MONDAY && System.currentTimeMillis() - Bot.data.leaderboardResetStamp > 48 * 60 * 60 * 1000) {
            Bot.data.leaderboardResetStamp = System.currentTimeMillis()

            val bufferWriter = BufferedWriter(FileWriter(dataFile.absoluteFile, false))
            val save = Bot.gson.toJson(Bot.data)
            bufferWriter.write(save)
            bufferWriter.close()

            Guild.clearLeaderboards()
        }
        Guild.saveAll()
        RequestBuffer.request { Bot.client.changePresence(StatusType.ONLINE, ActivityType.LISTENING, "the rain ${Bot.data.BOT_PREFIX}help") }
    }.repeat().execute()

    Discord4J.LOGGER.info("Registering events.")

    Bot.client = ClientBuilder().withToken(keys["discordToken"]!!).build()
    Bot.client.dispatcher.registerListener(VoteListener)
    Bot.client.dispatcher.registerListener(MessageListener)
    Bot.client.dispatcher.registerListener(VoiceListener)
    Bot.client.dispatcher.registerListener(Sounds)
    Bot.client.dispatcher.registerListener(CommandHandler)
    Bot.client.dispatcher.registerListener(Vote)
    Bot.client.dispatcher.registerListener(Bot)

    Discord4J.LOGGER.info("Logging in.")

    Bot.client.login()
}


