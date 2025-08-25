package me.arcator.onfimVelocity.timezone

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.time.ZoneId
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

object TZRequests {
    private const val IP_REQUEST_TEMPLATE =
        """{"requestType": "TIMEZONE_FROM_IP", "apiKey": "%s", "data": {"ip": "%s"}}"""
    private const val TZ_OVERRIDES_POST_TEMPLATE =
        """{"requestType": "TIMEZONE_OVERRIDES_POST", "apiKey": "%s", "data": {"%s": "%s"}}"""
    private const val TZ_OVERRIDES_GET_TEMPLATE =
        """{"requestType": "TIMEZONE_OVERRIDES_GET", "apiKey": "%s"}"""
    private const val TZ_OVERRIDE_REMOVE_TEMPLATE =
        """{"requestType": "TIMEZONE_OVERRIDE_REMOVE", "apiKey": "%s", "data": {"uuid": "%s"}}"""
    private const val USER_ID_UUID_LINK_POST_TEMPLATE =
        """{"requestType": "USER_ID_UUID_LINK_POST", "apiKey": "%s", "data": {"uuid": "%s", "timezone": "%s"}}"""
    private const val UUID_TIMEZONE_REQUEST =
        """{"requestType": "TIMEZONE_FROM_UUID", "apiKey": "%s", "data": {"uuid":"%s"}}"""
    private const val IS_LINKED_TEMPLATE =
        """{"requestType": "IS_LINKED", "apiKey": "%s", "data": {"uuid":"%s"}}"""
    private const val USER_ID_FROM_UUID_TEMPLATE =
        """{"requestType": "USER_ID_FROM_UUID", "apiKey": "%s", "data": {"uuid":"%s"}}"""

    fun sendIPTZRequest(ip: String): String? {
        val requestData = IP_REQUEST_TEMPLATE.format(Timezone.API_KEY, ip)
        val responseMap = sendTZBotRequest(requestData) ?: return null
        return responseMap["message"]!!.replace("\"", "")
    }

    fun sendTZFromUUID(uuid: UUID): String? {
        val requestData = UUID_TIMEZONE_REQUEST.format(Timezone.API_KEY, uuid.toString())
        val responseMap = sendTZBotRequest(requestData) ?: return null
        return responseMap["message"]!!.replace("\"", "")
    }

    fun sendTZOverridePost(uuid: UUID, timezone: String): Boolean {
        val requestData = TZ_OVERRIDES_POST_TEMPLATE.format(Timezone.API_KEY, uuid.toString(), timezone)
        val responseMap = sendTZBotRequest(requestData) ?: return false
        return responseMap["code"]!!.toInt() == 200
    }

    fun sendTZOverrideRemove(uuid: UUID): Boolean {
        val requestData = TZ_OVERRIDE_REMOVE_TEMPLATE.format(Timezone.API_KEY, uuid.toString())
        val responseMap = sendTZBotRequest(requestData) ?: return false
        return responseMap["code"]!!.toInt() == 200
    }

    fun sendUserIdUUIDLinkPost(uuid: UUID, timezone: ZoneId): String? {
        val requestData = USER_ID_UUID_LINK_POST_TEMPLATE.format(Timezone.API_KEY, uuid.toString(), timezone.toString())
        val responseMap = sendTZBotRequest(requestData) ?: return null
        if(responseMap["code"]!!.toInt() == 200) return responseMap["message"].toString().replace("\"", "")
        return null
    }

    fun sendTZOverridesRequest(): Map<UUID, String> {
        val responseMap = sendTZBotRequest(TZ_OVERRIDES_GET_TEMPLATE.format(Timezone.API_KEY)) ?: return mapOf()
        if(responseMap["code"]!!.toInt() == 200) {
            val gson = Gson()
            val tempMap: Map<String, String> = gson.fromJson(responseMap["message"], object : TypeToken<Map<String, String>>() {}.type)

            return tempMap.mapNotNull { (key, value) ->
                try {
                    UUID.fromString(key) to value
                } catch (e: IllegalArgumentException) {
                    null
                }
            }.toMap()
        }
        return mapOf()
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

    fun sendIsLinkedRequest(uuid: UUID): Boolean {
        val requestData = IS_LINKED_TEMPLATE.format(Timezone.API_KEY, uuid.toString())
        val responseMap = sendTZBotRequest(requestData) ?: return false
        return responseMap["code"]!!.toInt() == 200
    }

    fun sendUserIDFromUUID(uuid: UUID): String? {
        val requestData = USER_ID_FROM_UUID_TEMPLATE.format(Timezone.API_KEY, uuid.toString())
        val responseMap = sendTZBotRequest(requestData) ?: return null
        if(responseMap["code"]!!.toInt() == 200) return responseMap["message"]!!.replace("\"", "")
        return null
    }
}

