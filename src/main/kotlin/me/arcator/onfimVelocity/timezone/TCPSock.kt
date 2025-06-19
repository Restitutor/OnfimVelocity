package me.arcator.onfimVelocity.timezone

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

object TCPSock {
    private val ALIAS_REQUEST_TEMPLATE =
        """{"requestType": "RequestType.TIMEZONE_FROM_ALIAS_REQUEST", "data": {"alias":"%s"}}"""
    private val IP_REQUEST_TEMPLATE =
        """{"requestType": "RequestType.TIMEZONE_FROM_IP_REQUEST", "data": {"ip":"%s"}}"""


    @JvmStatic
    fun sendAliasTZRequest(playerName: String): String? {
        val requestData = ALIAS_REQUEST_TEMPLATE.format(playerName.lowercase())
        val responseMap = sendTZBotRequest(requestData) ?: return null
        return responseMap["message"]!!.replace("\"", "")
    }

    @JvmStatic
    fun sendIPTZRequest(ip: String): String? {
        val requestData = IP_REQUEST_TEMPLATE.format(ip)
        val responseMap = sendTZBotRequest(requestData) ?: return null
        return responseMap["message"]!!.replace("\"", "")
    }

    @JvmStatic
    private fun sendTZBotRequest(data: String): Map<String, String>? {
        return try {
            Socket("apollo", 8888).use { socket ->
                val out = PrintWriter(socket.getOutputStream(), true)
                out.println(data)
                out.flush()

                val response = BufferedReader(
                    InputStreamReader(
                        socket.getInputStream(),
                        StandardCharsets.UTF_8
                    )
                ).use { it.readText() }


                val jsonElement = Json.parseToJsonElement(response)
                val jsonObject = jsonElement.jsonObject

                val responseMap = jsonObject.mapValues { it.value.toString() }

                if (responseMap["code"]!!.toInt() != 200) {
                    return null
                }
                responseMap
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}

