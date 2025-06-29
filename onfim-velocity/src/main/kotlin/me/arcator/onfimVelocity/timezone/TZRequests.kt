package me.arcator.onfimVelocity.timezone

import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

object TZRequests {
    private const val ALIAS_REQUEST_TEMPLATE =
        """{"requestType": "RequestType.TIMEZONE_FROM_ALIAS_REQUEST", "data": {"alias":"%s"}}"""
    private const val IP_REQUEST_TEMPLATE =
        """{"requestType": "RequestType.TIMEZONE_FROM_IP_REQUEST", "data": {"ip":"%s"}}"""

    fun sendAliasTZRequest(playerName: String): String? {
        val requestData = ALIAS_REQUEST_TEMPLATE.format(playerName.lowercase())
        val responseMap = sendTZBotRequest(requestData) ?: return null
        return responseMap["message"]!!.replace("\"", "")
    }

    fun sendIPTZRequest(ip: String): String? {
        val requestData = IP_REQUEST_TEMPLATE.format(ip)
        val responseMap = sendTZBotRequest(requestData) ?: return null
        return responseMap["message"]!!.replace("\"", "")
    }

    private fun sendTZBotRequest(data: String): Map<String, String>? {
        return try {
            DatagramSocket().use { socket ->
                val address = InetAddress.getByName("apollo")
                val sendData = data.toByteArray(StandardCharsets.UTF_8)
                val sendPacket = DatagramPacket(sendData, sendData.size, address, 8888)
                socket.send(sendPacket)

                val buffer = ByteArray(4096)
                val receivePacket = DatagramPacket(buffer, buffer.size)
                socket.soTimeout = 3000
                socket.receive(receivePacket)
                val response = String(receivePacket.data, 0, receivePacket.length, StandardCharsets.UTF_8)

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

