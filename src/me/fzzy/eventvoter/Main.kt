package me.fzzy.eventvoter

import me.fzzy.eventvoter.commands.*
import me.fzzy.eventvoter.commands.help.*
import me.fzzy.eventvoter.thread.ImageProcessQueue
import me.fzzy.eventvoter.thread.IndividualTask
import me.fzzy.eventvoter.thread.Task
import org.im4java.core.ConvertCmd
import org.im4java.core.MogrifyCmd
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import java.util.*

lateinit var cli: IDiscordClient
lateinit var guilds: ArrayList<Leaderboard>
lateinit var commandHandler: CommandHandler
lateinit var convert: ConvertCmd
lateinit var mogrify: MogrifyCmd

lateinit var azureToken: String

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
lateinit var imageProcessQueue: ImageProcessQueue

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Please enter the bots tokens e.g. java -jar thisjar.jar discordtokenhere azuretokenhere")
        return
    }
    running = true
    azureToken = args[1]
    convert = ConvertCmd()
    mogrify = MogrifyCmd()
    mogrify.searchPath = "C:\\Program Files (x86)\\ImageMagick-6.9.9-Q16"
    convert.searchPath = "C:\\Program Files (x86)\\ImageMagick-6.9.9-Q16"

    val sounds = Sounds()

    commandHandler = CommandHandler(BOT_PREFIX)
    commandHandler.registerCommand("fzzy", Fzzy())
    commandHandler.registerCommand("eyes", Eyes())
    commandHandler.registerCommand("picture", Picture())
    commandHandler.registerCommand("emotion", Emotion())
    commandHandler.registerCommand("deepfry", Deepfry())
    commandHandler.registerCommand("mc", Mc())
    commandHandler.registerCommand("meme", Meme())
    commandHandler.registerCommand("mock", Mock())
    commandHandler.registerCommand("explode", Explode())

    commandHandler.registerCommand("leaderboard", LeaderboardCommand())

    commandHandler.registerCommand("pfp", Pfp())
    commandHandler.registerCommand("keep", Keep())

    commandHandler.registerCommand("help", Help())
    commandHandler.registerCommand("invite", Invite())
    commandHandler.registerCommand("sounds", sounds)
    commandHandler.registerCommand("eyetypes", Eyetypes())
    commandHandler.registerCommand("picturetypes", Picturetypes())
    commandHandler.registerCommand("mocks", Mocks())

    guilds = ArrayList()

    imageProcessQueue = ImageProcessQueue()
    imageProcessQueue.start()

    scheduler = Task()
    scheduler.registerTask(IndividualTask({
        if (!cli.isLoggedIn)
            cli.login()

        if (presenceActivityType != null && presenceStatusType != null && presenceText != null)
            RequestBuffer.request { cli.changePresence(presenceStatusType, presenceActivityType, presenceText) }

        val iter = guilds.iterator()
        while (iter.hasNext()) {
            val leaderboard = iter.next()
            var exists = false
            for (guild in cli.guilds) {
                if (guild.longID == leaderboard.leaderboardGuildId)
                    exists = true
            }
            if (!exists) {
                iter.remove()
                continue
            }
            if (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                if (leaderboard.weekWinner == null)
                    leaderboard.weekWinner = leaderboard.getCurrentWinner()
                if (leaderboard.weekWinner != null) {
                    if (System.currentTimeMillis() - leaderboard.weekWinner!!.timestamp > 1000 * 60 * 60 * 25 * 1) {
                        leaderboard.weekWinner = leaderboard.getCurrentWinner()
                        leaderboard.clearLeaderboard()
                    }
                }
            }

            leaderboard.saveLeaderboard()
            try {
                cli.ourUser.getVoiceStateForGuild(cli.getGuildByID(leaderboard.leaderboardGuildId)).channel?.leave()
            } catch (e: MissingPermissionsException) {
                println("Could not update leaderboard for guild")
                e.printStackTrace()
            } catch (e: DiscordException) {
                println("Could not update leaderboard for guild")
                e.printStackTrace()
            }
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

fun getLeaderboard(guildId: Long): Leaderboard? {
    for (leaderboard in guilds) {
        if (leaderboard.leaderboardGuildId == guildId)
            return leaderboard
    }
    return null
}




