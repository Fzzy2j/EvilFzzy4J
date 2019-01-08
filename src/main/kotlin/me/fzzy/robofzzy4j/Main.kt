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
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.RequestBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object RoboFzzy {
    lateinit var cli: IDiscordClient

    val savedMemesIds = ArrayList<Long>()

    lateinit var faceApiToken: String
    lateinit var speechApiToken: String

    const val BOT_PREFIX = "-"
    const val DEFAULT_TEMP_MESSAGE_DURATION: Long = 15 * 1000

    val UPVOTE_EMOJI = ReactionEmoji.of("upvote", 445376322353496064)!!
    val DOWNVOTE_EMOJI = ReactionEmoji.of("downvote", 445376330989830147)!!

    // Threads
    lateinit var auth: Authentication

    val random = Random()

    internal lateinit var guildFile: File
    lateinit var guildManager: ConfigurationLoader<CommentedConfigurationNode>
    lateinit var guildNode: ConfigurationNode

    internal lateinit var dataFile: File
    lateinit var dataManager: ConfigurationLoader<CommentedConfigurationNode>
    lateinit var dataNode: ConfigurationNode

    const val CONFIG_DIR: String = "data/"
    const val THREAD_COUNT = 12

    lateinit var executor: ExecutorService

    @EventSubscriber
    fun onReady(event: ReadyEvent) {
        Discord4J.LOGGER.info("list of current guilds:")
        for (guild in event.client.guilds) {
            Discord4J.LOGGER.info(guild.name)
        }
        RequestBuffer.request { RoboFzzy.cli.changePresence(StatusType.ONLINE, ActivityType.LISTENING, "the rain ${RoboFzzy.BOT_PREFIX}help") }
    }
}

fun main(args: Array<String>) {
    if (args.size != 3) {
        println("Please enter the bots tokens e.g. java -jar thisjar.jar discordtokenhere azurefacetokenhere azurespeechtokenhere")
        return
    }

    RoboFzzy.executor = Executors.newFixedThreadPool(RoboFzzy.THREAD_COUNT)

    Discord4J.LOGGER.info("Bot Starting.")

    RoboFzzy.guildFile = File(RoboFzzy.CONFIG_DIR + File.separator + "guilds.conf")
    RoboFzzy.dataFile = File(RoboFzzy.CONFIG_DIR + File.separator + "data.conf")
    RoboFzzy.guildFile.parentFile.mkdirs()
    RoboFzzy.guildManager = HoconConfigurationLoader.builder().setPath(RoboFzzy.guildFile.toPath()).build()
    RoboFzzy.dataManager = HoconConfigurationLoader.builder().setPath(RoboFzzy.dataFile.toPath()).build()
    RoboFzzy.guildNode = RoboFzzy.guildManager.load()
    RoboFzzy.dataNode = RoboFzzy.dataManager.load()

    RoboFzzy.faceApiToken = args[1]
    RoboFzzy.speechApiToken = args[2]
    RoboFzzy.auth = Authentication(RoboFzzy.speechApiToken)
    ProcessStarter.setGlobalSearchPath("C:\\Program Files\\ImageMagick-7.0.8-Q16")

    Discord4J.LOGGER.info("Registering commands.")

    CommandHandler.registerCommand("fzzy", Fzzy)
    CommandHandler.registerCommand("eyes", Eyes)
    CommandHandler.registerCommand("picture", Picture)
    CommandHandler.registerCommand("emotion", Emotion)
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
    CommandHandler.registerCommand("eyetypes", Eyetypes)
    CommandHandler.registerCommand("picturetypes", Picturetypes)
    CommandHandler.registerCommand("override", Override)

    Discord4J.LOGGER.info("Loading reviewIds.")
    RoboFzzy.savedMemesIds.clear()
    for (text in RoboFzzy.dataNode.getNode("savedMemesIds").getList(TypeToken.of(String::class.java))) {
        RoboFzzy.savedMemesIds.add(text.toLong())
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
        if (day == Calendar.MONDAY && System.currentTimeMillis() - RoboFzzy.dataNode.getNode("leaderboardResetTimestamp").long > 48 * 60 * 60 * 1000) {
            RoboFzzy.dataNode.getNode("leaderboardResetTimestamp").value = System.currentTimeMillis()
            RoboFzzy.dataManager.save(RoboFzzy.dataNode)
            Guild.clearLeaderboards()
        }
        Guild.saveAll()
        RequestBuffer.request { RoboFzzy.cli.changePresence(StatusType.ONLINE, ActivityType.LISTENING, "the rain ${RoboFzzy.BOT_PREFIX}help") }
    }, 60, true))

    Discord4J.LOGGER.info("Registering events.")

    RoboFzzy.cli = ClientBuilder().withToken(args[0]).build()
    RoboFzzy.cli.dispatcher.registerListener(VoteListener)
    RoboFzzy.cli.dispatcher.registerListener(MessageListener)
    RoboFzzy.cli.dispatcher.registerListener(VoiceListener)
    RoboFzzy.cli.dispatcher.registerListener(Sounds)
    RoboFzzy.cli.dispatcher.registerListener(CommandHandler)
    RoboFzzy.cli.dispatcher.registerListener(Vote)
    RoboFzzy.cli.dispatcher.registerListener(RoboFzzy)

    Discord4J.LOGGER.info("Logging in.")

    RoboFzzy.cli.login()
}


