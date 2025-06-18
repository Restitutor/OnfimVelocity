package me.arcator.onfimVelocity.timezone

import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletableFuture
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

object TCPSock {
    private val REQUEST_TEMPLATE =
        """{"requestType": "RequestType.TIMEZONE_FROM_ALIAS_REQUEST", "data": {"alias":"%s"}}"""

    @JvmStatic
    fun sendTZRequest(playerName: String): String? {
        return try {
            Socket("apollo", 8888).use { socket ->
                val requestData = REQUEST_TEMPLATE.format(playerName.lowercase())
                val out = PrintWriter(socket.getOutputStream(), true)
                out.println(requestData)
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
                responseMap["message"]?.replace("\"", "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

