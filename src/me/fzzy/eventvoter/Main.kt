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

private val CONSUMER_KEY = "6NZh2Vg6hpmBTOxsThxwAKpXG"
private val CONSUMER_KEY_SECRET = "j0vNhPVb08q9Frl4728OsXG1RmlsytGLuylHEbQB2voSxCShdS"
private val ACCESS_TOKEN = "983742933860929536-clmkeNujEHoA67K95Hm97pa2a12pRDs"
private val ACCESS_TOKEN_SECRET = "fHqRa1m7JAhtGXaWGZUeSddmxL04oh4KoJlSUyYzNbcKg"

fun main(args: Array<String>) {
    if (args.size != 1) {
        println("Please enter the bots token as the first argument e.g. java -jar thisjar.jar tokenhere")
        return
    }

    guilds = ArrayList()

    Task().start()

    cli = ClientBuilder().withToken(args[0]).build()
    cli.dispatcher.registerListener(Events())
    cli.login()
}

fun getLeaderboard(guildId: Long): Leaderboard? {
    for (leaderboard in guilds) {
        if (leaderboard.getGuildId() == guildId)
            return leaderboard
    }
    return null
}

class Sound constructor(userVoiceChannel: IVoiceChannel, audioP: AudioPlayer, audioDir: File, guild: IGuild) : Thread() {

    private var userVoiceChannel: IVoiceChannel
    private var audioP: AudioPlayer
    private var audioDir: File
    private var guild: IGuild

    init {
        this.userVoiceChannel = userVoiceChannel
        this.audioP = audioP
        this.audioDir = audioDir
        this.guild = guild
    }

    override fun run() {
        cli.ourUser.getVoiceStateForGuild(guild).channel?.leave()
        Thread.sleep(100)
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




