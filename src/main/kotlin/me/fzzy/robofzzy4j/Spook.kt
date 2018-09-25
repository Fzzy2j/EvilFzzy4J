package me.fzzy.robofzzy4j

import me.fzzy.robofzzy4j.listeners.VoiceListener
import me.fzzy.robofzzy4j.thread.IndividualTask
import sx.blah.discord.Discord4J
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IGuild
import sx.blah.discord.handle.obj.IVoiceChannel
import sx.blah.discord.util.audio.AudioPlayer
import sx.blah.discord.util.audio.events.TrackFinishEvent
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.sound.sampled.UnsupportedAudioFileException

class Spook {

    private val testVoice = 493253300582613024

    private var currentVoiceChannel: IVoiceChannel? = null

    private val spookyText = listOf(
            "Ṯ̷̟̞͎͂ú̵̘̋͐n̸͇̬͚͑̀̔͒ͅṉ̷̊̂̐ḛ̴̹̿́̆l̶̗̻̑́ ̶̻̦̘̺̅͠͠S̵̲̣̃͠n̵͉̖̤͝a̷̝̼̠͗̋̀k̷̢̗͙̚ę̶̐̓s̵̛͍̟̭̈́̈ ̴͉̟́̚Ċ̸̪̑̈́ọ̴̙̒ǫ̸̞͈̲̉̚l̶̢̦̑",
            "S̶̲̜̤͂͒̓̉ȟ̸̯͕̬̩̓͐ạ̷̠͌̔͆ḏ̵͆̉̿̚o̷͍̩͌̊w̴̞̪͓̃́ ̴̨̞̝̋Ŗ̶̟̮̕͝ẻ̷̲̮̓͠á̵̝̝͉̘l̴͖̺͉͋m̵̝̃͘͜",
            "Ļ̶̱̲̌̔î̴̫̔l̴̛͍̀ͅ ̵̛̣B̶̮̐̈́͝ì̷̝̒̽̚n̴͙̳̝̖̑̾k̷͙̺̓̾ͅ ̷̹̭͖̐̐̚ö̴̬̩̜́̉͜n̸̤̝̳͇̊̆̂ ̸̻̠͎̩͆t̶͚̅̽̉h̴̯̊̉̍e̵̹̍̑ ̶̧͝M̷͓͚͈̼̈i̴̫̥͔̓c̵̘̄͌"
    )

    private val code1Time = Time("code1")
    private val jumpTime = Time("jump")

    init {
        scheduler.registerTask(IndividualTask({
            if (System.currentTimeMillis() - code1Time.getNext() > 0) {
                if (!VoiceListener.interupt) {
                    val add = (random.nextInt(24) + 1) * 1000 * 60 * 60
                    code1Time.setNext(System.currentTimeMillis() + add + (random.nextInt(60) * 1000 * 60))

                    /*do {
                    currentVoiceChannel = guild().voiceChannels[random.nextInt(guild().voiceChannels.size)]
                } while (currentVoiceChannel?.connectedUsers?.size != 0)

                    currentVoiceChannel = guild().getVoiceChannelByID(testVoice)
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
                    }*/
                    type = "code1"
                    Discord4J.LOGGER.info("CODE 1 WOULD HAPPEN new time: ${SimpleDateFormat("dd:hh:mm:ss aa").format(Date(code1Time.getNext()))}")
                } else
                    code1Time.setNext(System.currentTimeMillis() + (1 * 1000 * 60))
            }
            if (System.currentTimeMillis() - jumpTime.getNext() > 0) {
                if (!VoiceListener.interupt) {
                    val add = (random.nextInt(24) + 1) * 1000 * 60 * 60
                    jumpTime.setNext(System.currentTimeMillis() + add + (random.nextInt(60) * 1000 * 60))

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

    @EventSubscriber
    fun onVoiceJoin(event: UserVoiceChannelJoinEvent) {
        if (currentVoiceChannel != null && !event.user.isBot) {
            if (event.voiceChannel.longID == currentVoiceChannel?.longID) {
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
            }
        }
    }

    @EventSubscriber
    fun onMessage(e: MessageReceivedEvent) {
        if (e.message.mentions.contains(cli.ourUser)) {
            val check = e.message.content.toLowerCase().replace(" ", "")
            if (check.contains("whoareyou")) {

            }
        }
        if (e.message.content.startsWith("-tests")) {
            Discord4J.LOGGER.info("test command")
            val newChannel = cli.getGuildByID(MEME_SERVER_ID).createVoiceChannel(spookyText[random.nextInt(spookyText.size)])
            VoiceListener.interupt = true
            currentVoiceChannel = newChannel
            newChannel.join()
        }
    }

    private var leaveOnFinish = false

    @EventSubscriber
    fun onTrackFinish(event: TrackFinishEvent) {
        if (currentVoiceChannel != null) {
            if (leaveOnFinish) {
                leaveOnFinish = false
                cli.ourUser.getVoiceStateForGuild(event.player.guild).channel?.leave()
                currentVoiceChannel = null
                VoiceListener.interupt = false
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