package me.fzzy.robofzzy4j

import com.google.common.reflect.TypeToken
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
import sx.blah.discord.util.DiscordException
import java.io.File
import java.util.*
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.loader.ConfigurationLoader
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import sx.blah.discord.handle.impl.obj.ReactionEmoji


lateinit var cli: IDiscordClient

val savedMemesIds = ArrayList<Long>()

lateinit var faceApiToken: String
lateinit var speechApiToken: String

const val BOT_PREFIX = "-"
const val DEFAULT_TEMP_MESSAGE_DURATION: Long = 15 * 1000

val UPVOTE_EMOJI = ReactionEmoji.of("upvote", 445376322353496064)!!
val DOWNVOTE_EMOJI = ReactionEmoji.of("downvote", 445376330989830147)!!

const val MEME_SERVER_ID = 214250278466224128

// Threads
lateinit var auth: Authentication

val random = Random()

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

    CommandHandler.registerCommand("getmeme", GetMeme)

    CommandHandler.registerCommand("leaderboard", LeaderboardCommand)

    CommandHandler.registerCommand("pfp", Pfp)

    CommandHandler.registerCommand("help", Help)
    CommandHandler.registerCommand("invite", Invite)
    CommandHandler.registerCommand("sounds", Sounds)
    CommandHandler.registerCommand("eyetypes", Eyetypes)
    CommandHandler.registerCommand("picturetypes", Picturetypes)

    Discord4J.LOGGER.info("Loading reviewIds.")
    savedMemesIds.clear()
    for (text in dataNode.getNode("savedMemesIds").getList(TypeToken.of(String::class.java))) {
        savedMemesIds.add(text.toLong())
    }

    for (file in File("cache").listFiles()) {
        file.delete()
    }

    Discord4J.LOGGER.info("Starting scheduler.")

    Task.start()

    Discord4J.LOGGER.info("Starting auto-saver.")

    Task.registerTask(IndividualTask({
        try {
            if (!cli.isLoggedIn)
                cli.login()
        } catch (e: DiscordException) {
        }

        val day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        if (day == Calendar.MONDAY && System.currentTimeMillis() - dataNode.getNode("leaderboardResetTimestamp").long > 48 * 60 * 60 * 1000) {
            dataNode.getNode("leaderboardResetTimestamp").value = System.currentTimeMillis()
            dataManager.save(dataNode)
            Guild.clearLeaderboards()
        }
        Guild.saveAll()
    }, 60, true))

    Discord4J.LOGGER.info("Registering events.")

    cli = ClientBuilder().withToken(args[0]).build()
    cli.dispatcher.registerListener(VoteListener)
    cli.dispatcher.registerListener(MessageListener)
    cli.dispatcher.registerListener(VoiceListener)
    cli.dispatcher.registerListener(StateListener)
    cli.dispatcher.registerListener(Sounds)
    cli.dispatcher.registerListener(CommandHandler)

    cli.dispatcher.registerListener(Spook())

    Discord4J.LOGGER.info("Logging in.")

    cli.login()
}



