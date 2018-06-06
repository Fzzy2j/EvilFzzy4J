package me.fzzy.eventvoter

import me.fzzy.eventvoter.commands.*
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient

lateinit var cli: IDiscordClient
lateinit var guilds: ArrayList<Leaderboard>

lateinit var commandHandler: CommandHandler

const val BOT_PREFIX = "-"

var running = false

const val OWNER_ID = 66104132028604416L

var imageQueues = 0

lateinit var azureToken: String

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

    commandHandler.registerCommand("help", Help())
    commandHandler.registerCommand("pfp", Pfp())
    commandHandler.registerCommand("sounds", sounds)

    guilds = ArrayList()

    Task().start()

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




