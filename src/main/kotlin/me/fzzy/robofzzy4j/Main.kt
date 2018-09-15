package me.fzzy.robofzzy4j

import me.fzzy.robofzzy4j.thread.IndividualTask
import me.fzzy.robofzzy4j.thread.Task
import me.fzzy.robofzzy4j.commands.*
import me.fzzy.robofzzy4j.commands.help.*
import me.fzzy.robofzzy4j.thread.Authentication
import org.im4java.core.ConvertCmd
import org.im4java.core.MogrifyCmd
import org.im4java.process.ProcessStarter
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.RequestBuffer
import java.io.File
import java.util.*

lateinit var cli: IDiscordClient
lateinit var guilds: ArrayList<Guild>
lateinit var commandHandler: CommandHandler

val reviewIds = ArrayList<Long>()
private val file: File = File("reviewIds.txt")

lateinit var faceApiToken: String
lateinit var speechApiToken: String

var imageQueues = 0
var running = false

const val BOT_PREFIX = "-"
const val OWNER_ID = 66104132028604416L
const val DEFAULT_TEMP_MESSAGE_DURATION: Long = 15 * 1000

private var presenceStatusType: StatusType? = null
private var presenceActivityType: ActivityType? = null
private var presenceText: String? = null

fun changeStatus(statusType: StatusType, activityType: ActivityType, text: String) {
    presenceStatusType = statusType
    presenceActivityType = activityType
    presenceText = text
    RequestBuffer.request { cli.changePresence(presenceStatusType, presenceActivityType, presenceText) }
}

lateinit var messageScheduler: MessageScheduler

// Threads
lateinit var scheduler: Task

lateinit var auth: Authentication

val random = Random()

var day = -1

fun main(args: Array<String>) {
    if (args.size != 3) {
        println("Please enter the bots tokens e.g. java -jar thisjar.jar discordtokenhere azurefacetokenhere azurespeechtokenhere")
        return
    }
    running = true
    faceApiToken = args[1]
    speechApiToken = args[2]
    auth = Authentication(speechApiToken)
    ProcessStarter.setGlobalSearchPath("C:\\Program Files\\ImageMagick-7.0.8-Q16")

    val sounds = Sounds()

    commandHandler = CommandHandler(BOT_PREFIX)
    commandHandler.registerCommand("fzzy", Fzzy())
    commandHandler.registerCommand("eyes", Eyes())
    commandHandler.registerCommand("picture", Picture())
    commandHandler.registerCommand("emotion", Emotion())
    commandHandler.registerCommand("deepfry", Deepfry())
    commandHandler.registerCommand("mc", Mc())
    commandHandler.registerCommand("explode", Explode())
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

    reviewIds.clear()
    if (file.exists()) {
        val serial = file.readText()
        for (score in 0 until serial.split(",").size - 1) {
            reviewIds.add(serial.split(",")[score].toLong())
        }
    }

    scheduler = Task()
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
            cli.getChannelByID(memeGeneralId).sendFile(list[random.nextInt(list.size)])
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
            if (reviewIds.size > 0) {
                var serial = ""

                for (i in reviewIds) {
                    serial += "$i,"
                }

                file.printWriter().use { out -> out.println(serial) }
            } else
                file.printWriter().use { out -> out.println() }
        }
    }, 60, true))
    scheduler.start()

    messageScheduler = MessageScheduler(scheduler)

    cli = ClientBuilder().withToken(args[0]).build()
    cli.dispatcher.registerListener(Upvote())
    cli.dispatcher.registerListener(sounds)
    cli.dispatcher.registerListener(commandHandler)
    cli.login()
}

fun getGuild(guildId: Long): Guild? {
    for (guild in guilds) {
        if (guild.longId == guildId)
            return guild
    }
    return null
}




