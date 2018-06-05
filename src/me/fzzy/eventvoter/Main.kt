package me.fzzy.eventvoter

import me.fzzy.eventvoter.commands.*
import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.audio.AudioPlayer
import java.io.File
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException

lateinit var cli: IDiscordClient
lateinit var guilds: ArrayList<Leaderboard>

lateinit var commandHandler: CommandHandler

const val BOT_PREFIX = "-"

var running = false

const val OWNER_ID = 66104132028604416L

var imageQueues = 0

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Please enter the bots tokens e.g. java -jar thisjar.jar tokenhere")
        return
    }
    running = true

    val sounds = Sounds()

    commandHandler = CommandHandler(BOT_PREFIX)
    commandHandler.registerCommand("fzzy", Fzzy())
    commandHandler.registerCommand("blend", Blend())
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

class TempMessage constructor(private var timeToStayMillis: Long, private var msg: IMessage) : Thread() {
    override fun run() {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeToStayMillis) {
            Thread.sleep(100L)
        }
        RequestBuffer.request { msg.delete() }
    }
}

class Sound constructor(private var userVoiceChannel: IVoiceChannel, private var audioP: AudioPlayer, private var audioDir: File, private var guild: IGuild) : Thread() {

    override fun run() {
        userVoiceChannel.join()
        Thread.sleep(100)
        audioP.clear()
        Thread.sleep(100)
        try {
            audioP.queue(audioDir)
        } catch (e: IOException) {
            // File not found
        } catch (e: UnsupportedAudioFileException) {
            e.printStackTrace()
        }
        while (audioP.currentTrack != null) {
            Thread.sleep(100)
        }
        cli.ourUser.getVoiceStateForGuild(guild).channel?.leave()
    }
}




