package me.fzzy.eventvoter

import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.util.audio.AudioPlayer
import java.io.File
import java.io.IOException
import javax.sound.sampled.UnsupportedAudioFileException
import twitter4j.auth.AccessToken
import twitter4j.TwitterStreamFactory
import twitter4j.TwitterFactory
import twitter4j.conf.ConfigurationBuilder
import twitter4j.TwitterStream

lateinit var cli: IDiscordClient
lateinit var guilds: ArrayList<Leaderboard>

lateinit var twitter: TwitterFactory
lateinit var stream: TwitterStream
lateinit var streamFactory: TwitterStreamFactory

private lateinit var CONSUMER_KEY: String
private lateinit var CONSUMER_KEY_SECRET: String
private lateinit var ACCESS_TOKEN: String
private lateinit var ACCESS_TOKEN_SECRET: String

fun main(args: Array<String>) {
    if (args.size != 5) {
        println("Please enter the bots tokens e.g. java -jar thisjar.jar tokenhere consumerkey consumerkeysecret accecsstoken accesstokensecret")
        return
    }

    CONSUMER_KEY = args[1]
    CONSUMER_KEY_SECRET = args[2]
    ACCESS_TOKEN = args[3]
    ACCESS_TOKEN_SECRET = args[4]

    val cb = ConfigurationBuilder()
    cb.setDebugEnabled(true)
            .setOAuthConsumerKey(CONSUMER_KEY)
            .setOAuthConsumerSecret(CONSUMER_KEY_SECRET)
            .setOAuthAccessToken(ACCESS_TOKEN)
            .setOAuthAccessTokenSecret(ACCESS_TOKEN_SECRET)
    val config = cb.build()
    twitter = TwitterFactory(config)
    streamFactory = TwitterStreamFactory(config)
    stream = streamFactory.getInstance(AccessToken(ACCESS_TOKEN, ACCESS_TOKEN_SECRET))
    T4JCompat.addStatusListener(stream, StatusUpdateListener())
    stream.user("OverwatchEU")

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




