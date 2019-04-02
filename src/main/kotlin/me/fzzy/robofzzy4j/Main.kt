package me.fzzy.robofzzy4j

import com.google.common.reflect.TypeToken
import me.fzzy.robofzzy4j.thread.IndividualTask
import me.fzzy.robofzzy4j.thread.Task
import me.fzzy.robofzzy4j.commands.*
import me.fzzy.robofzzy4j.commands.help.*
import me.fzzy.robofzzy4j.listeners.MessageListener
import me.fzzy.robofzzy4j.listeners.VoiceListener
import me.fzzy.robofzzy4j.listeners.VoteListener
import me.fzzy.robofzzy4j.thread.Authentication
import org.im4java.process.ProcessStarter
import sx.blah.discord.Discord4J
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import java.io.File
import java.util.*
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.utils.URIBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.json.JSONObject
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.RequestBuffer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.regex.Pattern

object Bot {
    lateinit var client: IDiscordClient

    val savedMemesIds = ArrayList<Long>()

    lateinit var speechApiToken: String

    const val BOT_PREFIX = "-"
    const val DEFAULT_TEMP_MESSAGE_DURATION: Long = 15 * 1000

    val UPVOTE_EMOJI = ReactionEmoji.of("upvote", 445376322353496064)!!
    val DOWNVOTE_EMOJI = ReactionEmoji.of("downvote", 445376330989830147)!!

    val URL_PATTERN = Pattern.compile("(?:^|[\\W])((ht|f)tp(s?):\\/\\/)"
            + "(([\\w\\-]+\\.){1,}?([\\w\\-.~]+\\/?)*"
            + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]\\*$~@!:/{};']*)")

    // Threads
    lateinit var azureAuth: Authentication

    val random = Random()

    lateinit var guildManager: ConfigurationLoader<CommentedConfigurationNode>
    lateinit var guildNode: ConfigurationNode

    lateinit var dataManager: ConfigurationLoader<CommentedConfigurationNode>
    lateinit var dataNode: ConfigurationNode

    const val CONFIG_DIR: String = "data/"
    const val THREAD_COUNT = 4

    lateinit var executor: ExecutorService

    @EventSubscriber
    fun onReady(event: ReadyEvent) {
        Discord4J.LOGGER.info("list of current guilds:")
        for (guild in event.client.guilds) {
            Discord4J.LOGGER.info(guild.name)
        }
        RequestBuffer.request { Bot.client.changePresence(StatusType.ONLINE, ActivityType.LISTENING, "the rain ${Bot.BOT_PREFIX}help") }
    }
}

fun main(args: Array<String>) {
    val keys = File("keys.conf")
    if (!keys.exists()) {
        Discord4J.LOGGER.warn("Generating new keys.conf file... you must enter a discord token for the bot to function")
        Files.copy(Bot::class.java.getResourceAsStream("keys.conf"), keys.toPath())
        System.exit(0)
    }
    val keyNode = HoconConfigurationLoader.builder().setPath(File("keys.conf").toPath()).build().load()
    Bot.speechApiToken = keyNode.getNode("speechApiToken").string!!

    Bot.executor = Executors.newFixedThreadPool(Bot.THREAD_COUNT)

    Discord4J.LOGGER.info("Bot Starting.")

    val guildFile = File(Bot.CONFIG_DIR + File.separator + "guilds.conf")
    val dataFile = File(Bot.CONFIG_DIR + File.separator + "data.conf")
    guildFile.parentFile.mkdirs()
    Bot.guildManager = HoconConfigurationLoader.builder().setPath(guildFile.toPath()).build()
    Bot.dataManager = HoconConfigurationLoader.builder().setPath(dataFile.toPath()).build()
    Bot.guildNode = Bot.guildManager.load()
    Bot.dataNode = Bot.dataManager.load()

    Bot.azureAuth = Authentication(Bot.speechApiToken)
    ProcessStarter.setGlobalSearchPath("C:\\Program Files\\ImageMagick-7.0.8-Q16")

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

    CommandHandler.registerCommand("pfp", Pfp)

    CommandHandler.registerCommand("help", Help)
    CommandHandler.registerCommand("invite", Invite)
    CommandHandler.registerCommand("sounds", Sounds)
    //CommandHandler.registerCommand("eyetypes", Eyetypes)
    CommandHandler.registerCommand("picturetypes", Picturetypes)
    CommandHandler.registerCommand("override", Override)

    Discord4J.LOGGER.info("Loading reviewIds.")
    Bot.savedMemesIds.clear()
    for (text in Bot.dataNode.getNode("savedMemesIds").getList(TypeToken.of(String::class.java))) {
        Bot.savedMemesIds.add(text.toLong())
    }

    val cacheFile = File("cache")
    if (cacheFile.exists()) {
        for (file in cacheFile.listFiles()) {
            file.deleteRecursively()
        }
    }

    Discord4J.LOGGER.info("Starting scheduler.")

    Task.start()

    Discord4J.LOGGER.info("Starting auto-saver.")

    Task.registerTask(IndividualTask({

        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (day == Calendar.MONDAY && System.currentTimeMillis() - Bot.dataNode.getNode("leaderboardResetTimestamp").long > 48 * 60 * 60 * 1000) {
            Bot.dataNode.getNode("leaderboardResetTimestamp").value = System.currentTimeMillis()
            Bot.dataManager.save(Bot.dataNode)
            Guild.clearLeaderboards()
        }
        Guild.saveAll()
        RequestBuffer.request { Bot.client.changePresence(StatusType.ONLINE, ActivityType.LISTENING, "the rain ${Bot.BOT_PREFIX}help") }
    }, 60, true))

    Discord4J.LOGGER.info("Registering events.")

    Bot.client = ClientBuilder().withToken(keyNode.getNode("discordToken").string!!).build()
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


