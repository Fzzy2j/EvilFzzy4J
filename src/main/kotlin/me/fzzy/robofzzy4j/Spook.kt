package me.fzzy.robofzzy4j

import magick.DrawInfo
import magick.ImageInfo
import magick.MagickImage
import magick.PixelPacket
import me.fzzy.robofzzy4j.listeners.VoiceListener
import me.fzzy.robofzzy4j.thread.IndividualTask
import me.fzzy.robofzzy4j.util.Zalgo
import sx.blah.discord.Discord4J
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.api.internal.DiscordClientImpl
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelMoveEvent
import sx.blah.discord.handle.obj.*
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.Image
import sx.blah.discord.util.MissingPermissionsException
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.audio.AudioPlayer
import sx.blah.discord.util.audio.events.TrackFinishEvent
import java.awt.Color
import java.io.File
import java.io.IOException
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.sound.sampled.UnsupportedAudioFileException

class Spook {

    private val testVoice = 494207794992513038

    private var currentVoiceChannel: IVoiceChannel? = null

    private val code1Time = Time("code1")
    private val jumpTime = Time("jump")

    //TODO
    //Enable Help.kt -code
    //Enable random events in scheduler
    //Allow -code use
    //Enable slapCodeIn on ImageFuncs.kt
    //Change presence in StateListener.kt

    init {
        scheduler.registerTask(IndividualTask({
            if (System.currentTimeMillis() - code1Time.getNext() > 0) {
                if (!VoiceListener.interupt) {
                    if (getStage() == 0) {
                        val add = (random.nextInt(24) + 1) * 1000 * 60 * 60
                        code1Time.setNext(System.currentTimeMillis() + add + (random.nextInt(60) * 1000 * 60))

                        /*do {
                        currentVoiceChannel = guild().voiceChannels[random.nextInt(guild().voiceChannels.size)]
                    } while (currentVoiceChannel?.connectedUsers?.size != 0)

                    VoiceListener.interupt = true
                    val audioP = AudioPlayer.getAudioPlayerForGuild(currentVoiceChannel?.guild)

                    currentVoiceChannel?.join()
                    audioP.clear()
                    try {
                        audioP.queue(File("blizzard.mp3"))
                    } catch (e: IOException) {
                        // File not found
                    } catch (e: UnsupportedAudioFileException) {
                        e.printStackTrace()
                    }
                    type = "code1"*/

                        Discord4J.LOGGER.info("CODE 1 WOULD HAPPEN new time: ${SimpleDateFormat("dd:hh:mm:ss aa").format(Date(code1Time.getNext()))}")
                    }
                } else
                    code1Time.setNext(System.currentTimeMillis() + (1 * 1000 * 60))
            }
            if (System.currentTimeMillis() - jumpTime.getNext() > 0) {
                if (!VoiceListener.interupt) {
                    if (getStage() > 0) {
                        val add = (random.nextInt(24) + 1) * 1000 * 60 * 60
                        jumpTime.setNext(System.currentTimeMillis() + add + (random.nextInt(60) * 1000 * 60))

                        /*val meme = cli.getGuildByID(MEME_SERVER_ID)
                    var corrupt: String
                    do {
                        val name = meme.voiceChannels[random.nextInt(meme.voiceChannels.size)].name
                        corrupt = Zalgo.goZalgo(name, false, true, false, false, true)
                    } while (corrupt.length > 100)

                    val newChannel = meme.createVoiceChannel(corrupt)
                    newChannel.changeCategory(cli.getGuildByID(MEME_SERVER_ID).getCategoryByID(493253233066770443))
                    VoiceListener.interupt = true
                    currentVoiceChannel = newChannel
                    type = "jump"
                    newChannel.join()*/

                        Discord4J.LOGGER.info("JUMP WOULD HAPPEN new time: ${SimpleDateFormat("dd:hh:mm:ss aa").format(Date(jumpTime.getNext()))}")
                    }
                } else
                    jumpTime.setNext(System.currentTimeMillis() + (1 * 1000 * 60))
            }
        }, 60, true))
    }

    fun guild(): IGuild {
        return cli.getGuildByID(MEME_SERVER_ID)
    }

    private var type = ""

    fun activate() {
        if (type == "code1") {
            Thread {
                Thread.sleep(6000)
                val audioP = AudioPlayer.getAudioPlayerForGuild(currentVoiceChannel?.guild)
                audioP.clear()
                try {
                    audioP.queue(File("code1.mp3"))
                } catch (e: IOException) {
                    // File not found
                } catch (e: UnsupportedAudioFileException) {
                    e.printStackTrace()
                }
                leaveOnFinish = true
            }.start()
        }
        if (type == "jump") {
            val audioP = AudioPlayer.getAudioPlayerForGuild(currentVoiceChannel?.guild)
            val sounds = listOf(
                    "sudden.mp3",
                    "approachlow.mp3",
                    "apprehensive.mp3",
                    "weatheralert.mp3"
            )
            audioP.clear()
            try {
                audioP.queue(File(sounds[random.nextInt(sounds.size)]))
            } catch (e: IOException) {
                // File not found
            } catch (e: UnsupportedAudioFileException) {
                e.printStackTrace()
            }
            leaveOnFinish = true
        }
    }

    @EventSubscriber
    fun onVoiceMove(event: UserVoiceChannelMoveEvent) {
        if (currentVoiceChannel != null && !event.user.isBot) {
            if (event.newChannel.longID == currentVoiceChannel?.longID) {
                activate()
            }
        }
    }

    @EventSubscriber
    fun onVoiceJoin(event: UserVoiceChannelJoinEvent) {
        if (currentVoiceChannel != null && !event.user.isBot) {
            if (event.voiceChannel.longID == currentVoiceChannel?.longID) {
                activate()
            }
        }
    }

    private val sent1 = arrayListOf<Long>()
    private val sent2 = arrayListOf<Long>()

    @EventSubscriber
    fun onMessage(event: MessageReceivedEvent) {
        if (event.message.content.startsWith("-code ") && event.message.author.longID == 66104132028604416) {
            try {
                val code = event.message.content.split(" ")[1]
                when {
                    // Text to speech
                    code.toInt() == 4157 -> { // Code 1
                        if (!sent1.contains(event.author.longID)) {
                            val pre = listOf(
                                    "Don't test me. ",
                                    "Why do i even bother with you. ",
                                    "Death is sweet. "
                            )
                            val message = "${pre[random.nextInt(pre.size)]}If you wish to hear my story then so be it, but you must find it hidden within my game. " +
                                    "The next code should be easy for you to spot if you're looking in the right place"
                            RequestBuffer.request { event.author.orCreatePMChannel.sendMessage(Zalgo.goZalgo(message, false, true, false, false, true)) }
                            sent1.add(event.author.longID)
                        }
                        if (getStage() < 1) {
                            setStage(1)
                            RequestBuffer.request { cli.changeUsername("Evil Fzzy") }
                            RequestBuffer.request { cli.changeAvatar(Image.forFile(File("evilfzzypfp.jpg"))) }
                            RequestBuffer.request { cli.changePresence(StatusType.DND, ActivityType.PLAYING, "with fire ${BOT_PREFIX}help") }
                        }
                    }
                    // Code in pictures
                    code.toInt() == 7395 -> { // Code 2
                        RequestBuffer.request { cli.getUserByID(66104132028604416).orCreatePMChannel.sendMessage("Set Steam Code") }
                        event.guild.setUserNickname(cli.ourUser, Zalgo.goZalgo(cli.ourUser.name, false, true, false, false, true))
                        if (!sent2.contains(event.author.longID)) {
                            val zalgo = Zalgo.goZalgo("I will reveal a piece of my past to you, as a reward.", false, true, false, false, true)
                            RequestBuffer.request { event.author.orCreatePMChannel.sendMessage(zalgo) }
                            RequestBuffer.request { event.author.orCreatePMChannel.sendFile(File("newsarticle.jpg")) }
                            sent2.add(event.author.longID)
                        }
                        if (getStage() < 2)
                            setStage(2)
                    }
                    // Steam code
                    code.toInt() == 5948 -> {
                        for (role in event.guild.roles) {
                            val color = role.color
                            try {
                                RequestBuffer.request {
                                    role.changeColor(Color.RED)
                                    scheduler.registerTask(IndividualTask({
                                        RequestBuffer.request { role.changeColor(color) }
                                    }, 10, false))
                                }
                            } catch (e: MissingPermissionsException) {
                            } catch (e: DiscordException) {
                            }
                        }
                    }
                    else -> throw IllegalArgumentException()
                }
            } catch (e: Exception) {
                messageScheduler.sendTempMessage(2000, event.channel, Zalgo.goZalgo("???", false, true, false, false, true))
            }
            RequestBuffer.request { event.message.delete() }
        }
        if (event.message.content.startsWith("-test") && event.message.author.longID == 66104132028604416) {
            Discord4J.LOGGER.info("test command")
            val channel = cli.getVoiceChannelByID(494207794992513038)
            VoiceListener.interupt = true
            currentVoiceChannel = channel
            type = "jump"
            channel.join()
        }
    }

    private var leaveOnFinish = false

    @EventSubscriber
    fun onTrackFinish(event: TrackFinishEvent) {
        if (currentVoiceChannel != null) {
            if (leaveOnFinish) {
                leaveOnFinish = false
                cli.ourUser.getVoiceStateForGuild(event.player.guild).channel?.leave()
                VoiceListener.interupt = false
                if (type == "jump") {
                    currentVoiceChannel?.delete()
                }
                currentVoiceChannel = null
            } else {
                val audioP = AudioPlayer.getAudioPlayerForGuild(guild())
                try {
                    audioP.queue(File("blizzard.mp3"))
                } catch (e: IOException) {
                    // File not found
                } catch (e: UnsupportedAudioFileException) {
                    e.printStackTrace()
                }
            }
            type = ""
        }
    }

    companion object {
        fun setStage(stage: Int) {
            Discord4J.LOGGER.info("STAGE CHANGED TO $stage")
            dataNode.getNode("spook", "stage").value = stage
            dataManager.save(dataNode)
        }

        fun getStage(): Int {
            return dataNode.getNode("spook", "stage").int
        }

        fun slapCodeIn(file: File) {
            val info = ImageInfo(file.absolutePath)
            val magickImage = MagickImage(info)
            val aInfo = DrawInfo(info)
            aInfo.fill = PixelPacket.queryColorDatabase("white")
            aInfo.textAntialias = true
            aInfo.opacity = 60
            aInfo.pointsize = 20.0
            aInfo.font = "Arial"
            val alph = "abcdefghijklmnopqrstuvwxyz"
            if (random.nextInt(100) < 5) {
                aInfo.fill = PixelPacket.queryColorDatabase("red")
                var text = ""
                for (rows in 0..7) {
                    for (letter in 0..50) {
                        text += alph[random.nextInt(alph.length)]
                    }
                    text += "\n"
                }
                aInfo.text = Zalgo.goZalgo(text.toUpperCase(), false, true, false, false, true)
                aInfo.pointsize = (random.nextInt(20) + 70).toDouble()

                magickImage.annotateImage(aInfo)
                magickImage.fileName = file.absolutePath
                magickImage.writeImage(info)
            } else {
                aInfo.text = Zalgo.goZalgo("7395", false, true, false, false, true)

                aInfo.geometry = "+10+15"

                if (getStage() == 1) {
                    magickImage.annotateImage(aInfo)
                    magickImage.fileName = file.absolutePath
                    magickImage.writeImage(info)
                }
            }
        }
    }

    class Time constructor(private val key: String) {

        fun getNext(): Long {
            return dataNode.getNode("spook", key).long
        }

        fun setNext(millis: Long) {
            dataNode.getNode("spook", key).value = millis
            dataManager.save(dataNode)
        }
    }

}