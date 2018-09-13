package me.fzzy.robofzzy4j.thread

import javax.net.ssl.HttpsURLConnection
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.util.Timer
import java.util.TimerTask

class Authentication(private val apiKey: String) {
    private var accessToken: String? = null
    private val accessTokenRenewer: Timer

    // Access Token expires every 10 minutes. Renew it every 9 minutes only.
    private val RefreshTokenDuration = 9 * 60 * 1000
    private var nineMinutesTask: TimerTask? = null

    init {

        val th = Thread(Runnable { RenewAccessToken() })

        try {
            th.start()
            th.join()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // renew the accessToken every specified minutes
        accessTokenRenewer = Timer()
        nineMinutesTask = object : TimerTask() {
            override fun run() {
                RenewAccessToken()
            }
        }

        accessTokenRenewer.schedule(nineMinutesTask!!, RefreshTokenDuration.toLong(), RefreshTokenDuration.toLong())
    }

    fun GetAccessToken(): String? {
        return this.accessToken
    }

    private fun RenewAccessToken() {
        synchronized(this) {
            HttpPost(AccessTokenUri, this.apiKey)
        }
    }

    private fun HttpPost(AccessTokenUri: String, apiKey: String) {
        var inSt: InputStream? = null
        var webRequest: HttpsURLConnection? = null

        this.accessToken = null
        //Prepare OAuth request
        try {
            val url = URL(AccessTokenUri)
            webRequest = url.openConnection() as HttpsURLConnection
            webRequest.doInput = true
            webRequest.doOutput = true
            webRequest.connectTimeout = 5000
            webRequest.readTimeout = 5000
            webRequest.setRequestProperty("Ocp-Apim-Subscription-Key", apiKey)
            webRequest.requestMethod = "POST"

            val request = ""
            val bytes = request.toByteArray()
            webRequest.setRequestProperty("content-length", bytes.size.toString())
            webRequest.connect()

            val dop = DataOutputStream(webRequest.outputStream)
            dop.write(bytes)
            dop.flush()
            dop.close()

            inSt = webRequest.inputStream
            val `in` = InputStreamReader(inSt!!)
            val bufferedReader = BufferedReader(`in`)
            val strBuffer = StringBuffer()
            var line: String? = null
            while (line != null) {
                line = bufferedReader.readLine()
                strBuffer.append(line)
            }

            bufferedReader.close()
            `in`.close()
            inSt.close()
            webRequest.disconnect()

            this.accessToken = strBuffer.toString()

        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    companion object {
        private val LOG_TAG = "Authentication"
        const val AccessTokenUri = "https://api.cognitive.microsoft.com/sts/v1.0/issueToken"
    }
}
