package me.fzzy.robofzzy4j

import com.google.common.reflect.TypeToken
import jdk.nashorn.internal.parser.TokenType
import me.fzzy.robofzzy4j.thread.IndividualTask
import me.fzzy.robofzzy4j.thread.Task
import me.fzzy.robofzzy4j.commands.*
import me.fzzy.robofzzy4j.commands.help.*
import me.fzzy.robofzzy4j.listeners.MessageListener
import me.fzzy.robofzzy4j.listeners.StateListener
import me.fzzy.robofzzy4j.listeners.VoiceListener
import me.fzzy.robofzzy4j.listeners.VoteListener
import me.fzzy.robofzzy4j.thread.Authentication
import org.im4java.process.ProcessStarter
import sx.blah.discord.Discord4J
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.RequestBuffer
import java.io.File
import java.util.*
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import java.io.IOException
import ninja.leaping.configurate.hocon.HoconConfigurationLoader


lateinit var cli: IDiscordClient
lateinit var guilds: ArrayList<Guild>
lateinit var commandHandler: CommandHandler

val reviewIds = ArrayList<Long>()

lateinit var faceApiToken: String
lateinit var speechApiToken: String

const val BOT_PREFIX = "-"
const val DEFAULT_TEMP_MESSAGE_DURATION: Long = 15 * 1000
const val MEME_REVIEW_ID = 485256221633413141
const val MEME_SERVER_ID = 214250278466224128
const val MEME_GENERAL_CHANNEL_ID = 397151198899339264

lateinit var messageScheduler: MessageScheduler

// Threads
lateinit var scheduler: Task

lateinit var auth: Authentication

val random = Random()

var day = -1

private lateinit var guildFile: File
lateinit var guildManager: ConfigurationLoader<CommentedConfigurationNode>
lateinit var guildNode: ConfigurationNode

private lateinit var dataFile: File
lateinit var dataManager: ConfigurationLoader<CommentedConfigurationNode>
lateinit var dataNode: ConfigurationNode

const val CONFIG_DIR: String = "data/"

fun main(args: Array<String>) {
    if (args.size != 3) {
        println("Please enter the bots tokens e.g. java -jar thisjar.jar discordtokenhere azurefacetokenhere azurespeechtokenhere")
        return
    }

    Discord4J.LOGGER.info("Bot Starting.")

    guildFile = File(CONFIG_DIR + File.separator + "guilds.conf")
    dataFile = File(CONFIG_DIR + File.separator + "data.conf")
    guildFile.parentFile.mkdirs()
    guildManager = HoconConfigurationLoader.builder().setPath(guildFile.toPath()).build()
    dataManager = HoconConfigurationLoader.builder().setPath(dataFile.toPath()).build()
    guildNode = guildManager.load()
    dataNode = dataManager.load()

    faceApiToken = args[1]
    speechApiToken = args[2]
    auth = Authentication(speechApiToken)
    ProcessStarter.setGlobalSearchPath("C:\\Program Files\\ImageMagick-7.0.8-Q16")

    val sounds = Sounds()

    Discord4J.LOGGER.info("Registering commands.")

    commandHandler = CommandHandler(BOT_PREFIX)
    commandHandler.registerCommand("fzzy", Fzzy())
    commandHandler.registerCommand("eyes", Eyes())
    commandHandler.registerCommand("picture", Picture())
    commandHandler.registerCommand("emotion", Emotion())
    commandHandler.registerCommand("deepfry", Deepfry())
    commandHandler.registerCommand("mc", Mc())
    commandHandler.registerCommand("explode", Explode())
    commandHandler.registerCommand("meme", Meme())
    commandHandler.registerCommand("play", Play())
    commandHandler.registerCommand("tts", Tts())

    commandHandler.registerCommand("leaderboard", LeaderboardCommand())

    commandHandler.registerCommand("pfp", Pfp())

    commandHandler.registerCommand("help", Help())
    commandHandler.registerCommand("invite", Invite())
    commandHandler.registerCommand("sounds", sounds)
    commandHandler.registerCommand("eyetypes", Eyetypes())
    commandHandler.registerCommand("picturetypes", Picturetypes())
    commandHandler.registerCommand("mocks", Mocks())

    guilds = ArrayList()

    Discord4J.LOGGER.info("Loading reviewIds.")
    reviewIds.clear()
    for (text in dataNode.getNode("reviewIds").getList(TypeToken.of(String::class.java))) {
        reviewIds.add(text.toLong())
    }

    for (file in File("cache").listFiles()) {
        file.delete()
    }

    Discord4J.LOGGER.info("Starting scheduler.")

    scheduler = Task()
    scheduler.start()

    Discord4J.LOGGER.info("Starting auto-saver.")

    scheduler.registerTask(IndividualTask({
        try {
            if (!cli.isLoggedIn)
                cli.login()
        } catch (e: DiscordException) {
        }

        if (presenceActivityType != null && presenceStatusType != null && presenceText != null)
            RequestBuffer.request { cli.changePresence(presenceStatusType, presenceActivityType, presenceText) }

        val date = Date(System.currentTimeMillis())
        if (day != date.day && date.hours == 16) {
            day = Date(System.currentTimeMillis()).day
            val list = File("memes").listFiles()
            cli.getChannelByID(MEME_GENERAL_CHANNEL_ID).sendFile(list[random.nextInt(list.size)])
        }

        val iter = guilds.iterator()
        while (iter.hasNext()) {
            val guild = iter.next()
            var exists = false
            for (guilds in cli.guilds) {
                if (guilds.longID == guild.longId)
                    exists = true
            }
            if (!exists) {
                iter.remove()
                continue
            }

            guild.save()
            var i = 0
            for (id in reviewIds) {
                dataNode.getNode("reviewIds", i++).value = id
            }
            dataManager.save(dataNode)
        }
    }, 60, true))

    messageScheduler = MessageScheduler(scheduler)

    Discord4J.LOGGER.info("Registering events.")

    cli = ClientBuilder().withToken(args[0]).build()
    cli.dispatcher.registerListener(StateListener())
    cli.dispatcher.registerListener(VoteListener())
    cli.dispatcher.registerListener(MessageListener())
    cli.dispatcher.registerListener(VoiceListener())
    cli.dispatcher.registerListener(sounds)
    cli.dispatcher.registerListener(commandHandler)
    cli.dispatcher.registerListener(Spook())

    Discord4J.LOGGER.info("Logging in.")

    cli.login()
}

private var presenceStatusType: StatusType? = null
private var presenceActivityType: ActivityType? = null
private var presenceText: String? = null

fun changeStatus(statusType: StatusType, activityType: ActivityType, text: String) {
    presenceStatusType = statusType
    presenceActivityType = activityType
    presenceText = text
    RequestBuffer.request { cli.changePresence(presenceStatusType, presenceActivityType, presenceText) }
}

fun getGuild(guildId: Long): Guild? {
    for (guild in guilds) {
        if (guild.longId == guildId)
            return guild
    }
    return null
}




