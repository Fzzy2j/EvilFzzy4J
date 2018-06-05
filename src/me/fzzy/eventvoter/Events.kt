package me.fzzy.eventvoter

import magick.ImageInfo
import magick.MagickImage
import me.fzzy.eventvoter.seam.BufferedImagePicture
import me.fzzy.eventvoter.seam.SeamCarver
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionAddEvent
import sx.blah.discord.handle.impl.events.guild.channel.message.reaction.ReactionRemoveEvent
import sx.blah.discord.handle.impl.obj.ReactionEmoji
import sx.blah.discord.handle.obj.ActivityType
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IMessage
import sx.blah.discord.handle.obj.StatusType
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.RequestBuilder
import java.util.regex.Matcher
import java.util.regex.Pattern
import sx.blah.discord.util.audio.AudioPlayer
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.ImageIcon

const val FZZY_ID = 66104132028604416L
const val MEMES_ID = 214250278466224128L

lateinit var carver: SeamCarver

class Events {

    init {
        carver = SeamCarver()
    }

    private var cooldowns: HashMap<Long, Long> = hashMapOf()

    @EventSubscriber
    fun onMessageReceived(event: MessageReceivedEvent) {
        if (!event.message.author.isBot) {
            if (getLeaderboard(event.guild.longID) == null)
                guilds.add(Leaderboard(event.guild.longID))
            val pattern = Pattern.compile("((http:\\/\\/|https:\\/\\/)?(www.)?(([a-zA-Z0-9-]){2,}\\.){1,4}([a-zA-Z]){2,6}(\\/([a-zA-Z-_\\/\\.0-9#:?=&;,]*)?)?)")
            val m: Matcher = pattern.matcher(event.message.content)
            if (m.find() || event.message.attachments.size > 0) {
                RequestBuilder(event.client).shouldBufferRequests(true).doAction {
                    event.message.addReaction(ReactionEmoji.of("upvote", 445376322353496064L))
                    true
                }.andThen {
                    event.message.addReaction(ReactionEmoji.of("downvote", 445376330989830147L))
                    true
                }.execute()
            }

            // Commands for memes server and stop command
            val soundCooldown = 30
            if (event.message.content.startsWith("-") && event.message.content.length > 1) {

                // Stop the bot
                if (event.message.content.equals("-stop", true)) {
                    try {
                        RequestBuffer.request { event.message.delete() }
                    } catch (e: MissingPermissionsException) {
                    }
                    if (event.author.longID == FZZY_ID) {
                        for (leaderboard in guilds) {
                            leaderboard.saveLeaderboard()
                        }
                        cli.logout()
                        running = false
                        System.exit(0)
                    }
                }

                //Image Seam Carving
                if (event.message.content.startsWith("-fzzy", true)) {
                }
                if (event.message.content.equals("-all", true)) {
                    RequestBuffer.request {
                        try {
                            event.message.delete()
                        } catch (e: MissingPermissionsException) {
                        }
                    }
                    var all = ""
                    for (file in File("sounds").listFiles()) {
                        all += "-${file.nameWithoutExtension}\n"
                    }
                    event.message.author.orCreatePMChannel.sendMessage(all)
                    return
                }
                if (cli.ourUser.getVoiceStateForGuild(event.guild).channel == null) {
                    if (System.currentTimeMillis() - cooldowns.getOrDefault(event.author.longID, 0) > soundCooldown * 1000) {
                        val userVoiceChannel = event.author.getVoiceStateForGuild(event.guild).channel ?: return
                        val audioP = AudioPlayer.getAudioPlayerForGuild(event.guild)
                        val audioDir = File("sounds").listFiles { file -> file.name.contains(event.message.content.substring(1)) }

                        if (audioDir == null || audioDir.isEmpty())
                            return

                        RequestBuffer.request {
                            try {
                                event.message.delete()
                            } catch (e: MissingPermissionsException) {
                            }
                        }

                        Sound(userVoiceChannel, audioP, audioDir[0], event.guild).start()
                        cooldowns[event.author.longID] = System.currentTimeMillis()
                    } else {
                        RequestBuffer.request {
                            try {
                                event.message.delete()
                            } catch (e: MissingPermissionsException) {
                            }
                        }
                        val timeLeft = soundCooldown - ((System.currentTimeMillis() - cooldowns.getOrDefault(event.author.longID, System.currentTimeMillis())) / 1000)
                        val message = "${event.author.getDisplayName(event.guild)}! You are on cooldown for $timeLeft seconds."
                        RequestBuffer.request { CooldownMessage(timeLeft.toInt(), event.channel, event.author.getDisplayName(event.guild), event.channel.sendMessage(message)).start() }
                    }
                }

            }
        }
    }

    @EventSubscriber
    fun onReactionAdd(event: ReactionAddEvent) {
        val leaderboard = getLeaderboard(event.guild.longID)
        if (leaderboard != null) {
            if (System.currentTimeMillis() / 1000 - event.message.timestamp.epochSecond < 60 * 60 * 24) {
                if (event.reaction.getUserReacted(cli.ourUser)) {
                    if (event.message.author.longID != event.user.longID) {
                        when (event.reaction.emoji.name) {
                            "upvote" -> leaderboard.addToScore(event.author.longID, 1)
                            "downvote" -> leaderboard.addToScore(event.author.longID, -1)
                        }
                    }
                }
            }
        }
    }

    @EventSubscriber
    fun onReactionRemove(event: ReactionRemoveEvent) {
        val leaderboard = getLeaderboard(event.guild.longID)
        if (leaderboard != null) {
            if (System.currentTimeMillis() / 1000 - event.message.timestamp.epochSecond < 60 * 60 * 24) {
                if (event.reaction.getUserReacted(cli.ourUser)) {
                    if (event.message.author.longID != event.user.longID) {
                        when (event.reaction.emoji.name) {
                            "upvote" -> leaderboard.addToScore(event.author.longID, -1)
                            "downvote" -> leaderboard.addToScore(event.author.longID, 1)
                        }
                    }
                }
            }
        }
    }

    @EventSubscriber
    fun onReady(event: ReadyEvent) {
        for (guild in cli.guilds) {
            val leaderboard = Leaderboard(guild.longID)
            guilds.add(leaderboard)
            leaderboard.loadLeaderboard()
            leaderboard.updateLeaderboard()
        }
        changeStatus(StatusType.ONLINE, ActivityType.LISTENING, "the rain -all")
    }


}

fun isImage(imgPath: URL): Boolean {
    return try {
        val image = ImageIO.read(imgPath)
        println(image != null)
        image != null
    } catch (e: Exception) {
        println("exception in isImage")
        false
    }
}

class CooldownMessage constructor(private var cooldown: Int, private var channel: IChannel, private var userName: String, private var msg: IMessage) : Thread() {
    override fun run() {
        while (cooldown > 0) {
            if (cooldown % 5 == 0) {
                RequestBuffer.request { msg.edit("$userName! You are on cooldown for $cooldown seconds.") }
            }
            Thread.sleep(1000L)
            cooldown--
        }
        RequestBuffer.request { msg.edit("$userName, you are no longer on cooldown.") }
        Thread.sleep(7000L)
        msg.delete()
    }
}
