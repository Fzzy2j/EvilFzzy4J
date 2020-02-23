package me.fzzy.evilfzzy4j

import com.google.gson.GsonBuilder
import com.google.gson.annotations.Expose
import com.google.gson.stream.JsonReader
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Message
import org.json.JSONObject
import java.io.*
import java.util.*

class FzzyGuild private constructor() {

    companion object {
        private val guilds = arrayListOf<FzzyGuild>()

        fun getGuild(guildId: String): FzzyGuild {
            for (guild in guilds) {
                if (guild.guildId == guildId)
                    return guild
            }
            val guildFile = File(Bot.DATA_FILE, "${guildId}.json")
            val guild = if (guildFile.exists()) {
                    Bot.gson.fromJson(JsonReader(InputStreamReader(guildFile.inputStream())), FzzyGuild::class.java)
            } else FzzyGuild()
            guild.guildId = guildId
            guilds.add(guild)
            return guild
        }

        fun saveAll() {
            for (guild in guilds) {
                guild.save()
            }
        }
    }

    private lateinit var guildId: String

    @Expose
    private val savedMessageIds = ArrayList<Long>()

    fun saveMessage(message: Message): File? {
        if (!savedMessageIds.contains(message.idLong)) {
            savedMessageIds.add(message.idLong)

            val url = Bot.getMessageMedia(message) ?: return null
            val suffixFinder = url.toString().split(".")
            val suffix = suffixFinder[suffixFinder.size - 1]

            File("memes", guildId).mkdirs()
            val fileName = "memes/${guildId}/${System.currentTimeMillis()}.$suffix"
            try {
                val openConnection = url.openConnection()
                openConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11")
                openConnection.connect()

                val inputStream = BufferedInputStream(openConnection.getInputStream())
                val outputStream = BufferedOutputStream(FileOutputStream(fileName))

                for (out in inputStream.iterator()) {
                    outputStream.write(out.toInt())
                }
                inputStream.close()
                outputStream.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return File(fileName)
        }
        return null
    }

    fun getDiscordGuild(): Guild? {
        return Bot.client.getGuildById(guildId)
    }

    fun save() {
        val gson = GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
        val guildFile = File(Bot.DATA_FILE, "${guildId}.json")
        val bufferWriter = BufferedWriter(FileWriter(guildFile.absoluteFile, false))
        val save = JSONObject(gson.toJson(this))
        bufferWriter.write(save.toString(2))
        bufferWriter.close()
    }

}