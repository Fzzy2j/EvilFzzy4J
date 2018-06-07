package me.fzzy.eventvoter

import me.fzzy.eventvoter.commands.*
import me.fzzy.eventvoter.commands.help.Eyetypes
import me.fzzy.eventvoter.commands.help.Help
import me.fzzy.eventvoter.commands.help.Sounds
import me.fzzy.eventvoter.thread.ImageProcessQueue
import me.fzzy.eventvoter.thread.IndividualTask
import me.fzzy.eventvoter.thread.Task
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

lateinit var scheduler: Task
lateinit var messageScheduler: MessageScheduler
lateinit var imageProcessQueue: ImageProcessQueue

fun main(args: Array<String>) {
    if (args.size != 2) {
        println("Please enter the bots tokens e.g. java -jar thisjar.jar discordtokenhere azuretokenhere")
        return
    }
    running = true
    azureToken = args[1]

    val sounds = Sounds()

    commandHandler = CommandHandler(BOT_PREFIX)
    commandHandler.registerCommand("fzzy", Fzzy())
    commandHandler.registerCommand("eyes", Eyes())
    commandHandler.registerCommand("emotion", Emotion())
    commandHandler.registerCommand("deepfry", Deepfry())
    commandHandler.registerCommand("mc", Mc())

    commandHandler.registerCommand("pfp", Pfp())

    commandHandler.registerCommand("help", Help())
    commandHandler.registerCommand("sounds", sounds)
    commandHandler.registerCommand("eyetypes", Eyetypes())

    guilds = ArrayList()

    imageProcessQueue = ImageProcessQueue()
    imageProcessQueue.start()

    scheduler = Task()
    scheduler.registerTask(IndividualTask({
        if (presenceActivityType != null && presenceStatusType != null && presenceText != null)
            RequestBuffer.request { cli.changePresence(presenceStatusType, presenceActivityType, presenceText) }

        for (leaderboard in guilds) {
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
                leaderboard.updateLeaderboard()
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




