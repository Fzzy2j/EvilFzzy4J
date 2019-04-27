package me.fzzy.robofzzy4j

import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import me.fzzy.robofzzy4j.commands.*
import me.fzzy.robofzzy4j.commands.help.Help
import me.fzzy.robofzzy4j.commands.help.Invite
import me.fzzy.robofzzy4j.commands.help.Picturetypes
import me.fzzy.robofzzy4j.commands.help.Sounds
import me.fzzy.robofzzy4j.listeners.MessageListener
import me.fzzy.robofzzy4j.listeners.VoiceListener
import me.fzzy.robofzzy4j.listeners.VoteListener
import me.fzzy.robofzzy4j.thread.Authentication
import me.fzzy.robofzzy4j.thread.Scheduler
import me.fzzy.robofzzy4j.util.MediaType
import org.im4java.process.ProcessStarter
import sx.blah.discord.Discord4J
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.InputStreamReader
import java.net.URL
import java.nio.file.Files
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern

class BotData {
    val BOT_PREFIX = "-"
    val DEFAULT_TEMP_MESSAGE_DURATION: Long = 15 * 1000
    val THREAD_COUNT = 4
    val IMAGE_MAGICK_DIRECTORY = "C:${File.separator}Program Files${File.separator}ImageMagick-7.0.8-Q16"
    val CURRENCY_EMOJI_NAME = "diamond"
    val CURRENCY_EMOJI_ID = 571593175907434516
}

object Bot {
    lateinit var client: IDiscordClient

    val gson = Gson()

    var speechApiToken: String? = null

    lateinit var data: BotData

    lateinit var CURRENCY_EMOJI: ReactionEmoji

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
        Discord4J.LOGGER.info("RoboFzzy v${Bot::class.java.`package`.implementationVersion} online.")
        RequestBuffer.request { Bot.client.changePresence(StatusType.ONLINE, ActivityType.LISTENING, "the rain ${Bot.data.BOT_PREFIX}help") }
    }

    @EventSubscriber
    fun onJoin(event: GuildCreateEvent) {
        Discord4J.LOGGER.info("guild created: ${event.guild.name}")
    }

    fun sendMessage(channel: IChannel, text: String): IMessage? {
        if (text.isEmpty())
            return null
        return try {
            channel.sendMessage(text)
        } catch (e: MissingPermissionsException) {
            null
        }
    }

    fun sendEmbed(channel: IChannel, embed: EmbedObject): IMessage? {
        return try {
            channel.sendMessage(embed)
        } catch (e: MissingPermissionsException) {
            null
        }
    }

    fun getMessageMediaUrl(message: IMessage, type: MediaType): URL? {
        var url: URL? = null
        if (message.attachments.size > 0) {
            url = URL(message.attachments[0].url)
        } else {
            for (split in message.content.split(" ")) {
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
        return url
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

    Discord4J.LOGGER.info("Loading data.")
    val dataFile = File("${Bot.DATA_DIR}config.json")
    if (dataFile.exists()) Bot.data = Bot.gson.fromJson(JsonReader(InputStreamReader(dataFile.inputStream())), BotData::class.java)
    else {
        Bot.data = BotData()
        val bufferWriter = BufferedWriter(FileWriter(dataFile.absoluteFile, false))
        val save = Bot.gson.toJson(Bot.data)
        bufferWriter.write(save)
        bufferWriter.close()
    }
    Bot.CURRENCY_EMOJI = ReactionEmoji.of(Bot.data.CURRENCY_EMOJI_NAME, Bot.data.CURRENCY_EMOJI_ID)!!

    Bot.speechApiToken = keys["speechApiToken"]

    Bot.executor = Executors.newFixedThreadPool(Bot.data.THREAD_COUNT)

    Discord4J.LOGGER.info("Bot Starting.")

    if (Bot.speechApiToken != null) Bot.azureAuth = Authentication(Bot.speechApiToken!!)
    ProcessStarter.setGlobalSearchPath(Bot.data.IMAGE_MAGICK_DIRECTORY)

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


