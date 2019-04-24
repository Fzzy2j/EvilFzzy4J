package me.fzzy.robofzzy4j

import me.fzzy.robofzzy4j.util.MediaType
import sx.blah.discord.handle.obj.IMessage
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.file.Files

object ImageHelper {

    fun downloadTempFile(url: URL): File? {
        val suffixFinder = url.toString().split(".")
        val suffix = ".${suffixFinder[suffixFinder.size - 1]}"

        val fileName = "cache/${System.currentTimeMillis()}.$suffix"
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
            return null
        }
        return File(fileName)
    }

    fun createTempFile(file: File?): File? {
        if (file == null) return null
        val new = File("cache/${System.currentTimeMillis()}.${file.extension}")
        Files.copy(file.toPath(), new.toPath())
        return new
    }

    fun getFirstImage(list: MutableList<IMessage>): URL? {
        for (message in list) {
            val url = Bot.getMessageMediaUrl(message, MediaType.IMAGE)
            if (url != null) return url
        }
        return null
    }
}