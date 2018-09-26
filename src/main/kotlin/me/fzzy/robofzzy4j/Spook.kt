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
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelMoveEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.util.RequestBuffer
import sx.blah.discord.util.audio.AudioPlayer
import sx.blah.discord.util.audio.events.TrackFinishEvent
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

    private var stage = 0

    //TODO
    //Enable Help.kt -code
    //Enable random events in scheduler
    //Allow -code use

    init {
        scheduler.registerTask(IndividualTask({
            if (System.currentTimeMillis() - code1Time.getNext() > 0) {
                if (!VoiceListener.interupt) {
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
                } else
                    code1Time.setNext(System.currentTimeMillis() + (1 * 1000 * 60))
            }
            if (System.currentTimeMillis() - jumpTime.getNext() > 0) {
                if (!VoiceListener.interupt) {
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

    val sent = arrayListOf<Long>()

    @EventSubscriber
    fun onMessage(event: MessageReceivedEvent) {
        if (event.message.content.startsWith("-code ") && event.message.author.longID == 66104132028604416) {
            try {
                val code = event.message.content.split(" ")[1]
                when {
                    code.toInt() == 4157 -> { // Code 1
                        if (!sent.contains(event.author.longID)) {
                            val pre = listOf(
                                    "I hate you, but i can't live without you. ",
                                    "Why do i even bother with you. ",
                                    "Why am i the one that's dead. "
                            )
                            val message = "${pre[random.nextInt(pre.size)]}If you wish to hear my story then so be it, but you must find it hidden within my game. " +
                                    "The next code should be easy for you to spot if you're looking in the right place"
                            event.author.orCreatePMChannel.sendMessage(Zalgo.goZalgo(message, false, true, false, false, true))
                            sent.add(event.author.longID)
                        }
                    }
                    code.toInt() == 7395 -> { // Code 2

                    }
                    else -> throw IllegalArgumentException()
                }
            } catch (e: Exception) {
                messageScheduler.sendTempMessage(2000, event.channel, Zalgo.goZalgo("???", false, true, false, false, true))
            }
            RequestBuffer.request { event.message.delete() }
        }
        if (event.message.content.startsWith("-tests") && event.message.author.longID == 66104132028604416) {
            Discord4J.LOGGER.info("test command")

            val history = event.channel.getMessageHistory(10).toMutableList()
            history.add(0, event.message)

            val url: URL = ImageFuncs.getFirstImage(history) ?: return
            val file = ImageFuncs.downloadTempFile(url) ?: return

            val info = ImageInfo(file.absolutePath)
            val magickImage = MagickImage(info)

            slapCodeIn(magickImage, info)

            magickImage.fileName = file.absolutePath
            magickImage.writeImage(info)
            RequestBuffer.request {
                Funcs.sendFile(event.channel, file)
                file.delete()
            }
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

    fun slapCodeIn(magickImage: MagickImage, info: ImageInfo) {
        val aInfo = DrawInfo(info)
        aInfo.fill = PixelPacket.queryColorDatabase("white")
        aInfo.textAntialias = true
        aInfo.opacity = 60
        aInfo.pointsize = 20.0
        aInfo.font = "Arial"
        aInfo.text = Zalgo.goZalgo("7395", false, true, false, false, true)

        aInfo.geometry = "+10+15"

        magickImage.annotateImage(aInfo)
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