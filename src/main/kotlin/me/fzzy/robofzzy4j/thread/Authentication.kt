package me.fzzy.robofzzy4j.thread

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.Timer
import java.util.TimerTask

import javax.net.ssl.HttpsURLConnection

/*
 * This class demonstrates how to get a valid O-auth token from
 * Azure Data Market.
 */
class Authentication(private val apiKey: String) {
    private var accessToken: String? = null
    private val accessTokenRenewer: Timer

    //Access token expires every 10 minutes. Renew it every 9 minutes only.
    private val RefreshTokenDuration = 9 * 60 * 1000
    private val charsetName = "utf-8"
    private var nineMinitesTask: TimerTask? = null

    init {

        this.accessToken = HttpPost(AccessTokenUri, this.apiKey)

        // renew the token every specified minutes
        accessTokenRenewer = Timer()
        nineMinitesTask = object : TimerTask() {
            override fun run() {
                RenewAccessToken()
            }
        }

        accessTokenRenewer.schedule(nineMinitesTask!!, 0, RefreshTokenDuration.toLong())
    }

    fun GetAccessToken(): String? {
        return this.accessToken
    }

    private fun RenewAccessToken() {
        val newAccessToken = HttpPost(AccessTokenUri, this.apiKey)
        //swap the new token with old one
        //Note: the swap is thread unsafe
        //System.out.println("new access token: " + accessToken);
        this.accessToken = newAccessToken
    }

    private fun HttpPost(AccessTokenUri: String, apiKey: String): String? {
        var inSt: InputStream?
        var webRequest: HttpsURLConnection?

        //Prepare OAuth request
        try {
            val url = URL(AccessTokenUri)
            webRequest = url.openConnection() as HttpsURLConnection
            webRequest.doInput = true
            webRequest.doOutput = true
            webRequest.connectTimeout = 5000
            webRequest.readTimeout = 5000
            webRequest.requestMethod = "POST"

            val bytes = ByteArray(0)
            webRequest.setRequestProperty("content-length", bytes.size.toString())
            webRequest.setRequestProperty("Ocp-Apim-Subscription-Key", apiKey)
            webRequest.connect()

            val dop = DataOutputStream(webRequest.outputStream)
            dop.write(bytes)
            dop.flush()
            dop.close()

            inSt = webRequest.inputStream
            val `in` = InputStreamReader(inSt!!)
            val bufferedReader = BufferedReader(`in`)
            val strBuffer = StringBuffer()
            for (line in bufferedReader.lines()) {
                strBuffer.append(line)
            }

            bufferedReader.close()
            `in`.close()
            inSt.close()
            webRequest.disconnect()

            // parse the access token

            return strBuffer.toString()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    companion object {
        val AccessTokenUri = "https://api.cognitive.microsoft.com/sts/v1.0/issueToken"
    }
}
