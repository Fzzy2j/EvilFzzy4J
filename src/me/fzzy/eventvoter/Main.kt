package me.fzzy.eventvoter

import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.util.audio.AudioPlayer
import java.io.File
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException

lateinit var cli: IDiscordClient
lateinit var guilds: ArrayList<Leaderboard>

var running = false

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Please enter the bots tokens e.g. java -jar thisjar.jar tokenhere")
        return
    }
    running = true

    guilds = ArrayList()

    Task().start()

    cli = ClientBuilder().withToken(args[0]).build()
    cli.dispatcher.registerListener(Events())
    cli.login()
}

fun getLeaderboard(guildId: Long): Leaderboard? {
    for (leaderboard in guilds) {
        if (leaderboard.leaderboardGuildId == guildId)
            return leaderboard
    }
    return null
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
        while (true) {
            Thread.sleep(100)
            if (audioP.currentTrack == null)
                break
        }
        cli.ourUser.getVoiceStateForGuild(guild).channel?.leave()
    }
}




