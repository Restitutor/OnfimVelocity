package me.arcator.onfimVelocity.chatXP

import com.m3z0id.tzbot4j.TZBot4J
import com.m3z0id.tzbot4j.network.UDPSocket
import com.m3z0id.tzbot4j.tzLib.net.TZRequest
import com.m3z0id.tzbot4j.tzLib.net.TZResponse
import com.m3z0id.tzbot4j.tzLib.net.c2s.UserIDFromUUIDData
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.util.*
import java.util.function.BiConsumer
import kotlin.RuntimeException
import kotlin.Throwable

class ChatXPHandler(tzbot: TZBot4J) {
    private val sock: UDPSocket
    private val tzbot: TZBot4J
    private val discordIdUUIDMap: MutableMap<UUID?, Long?>
    private var initialized = false

    init {
        try {
            this.sock = UDPSocket("jylina", 8890)
            this.discordIdUUIDMap = HashMap<UUID?, Long?>()
            this.tzbot = tzbot
            initialized = true
        } catch (e: SocketException) {
            initialized = false
            throw RuntimeException(e)
        } catch (e: UnknownHostException) {
            initialized = false
            throw RuntimeException(e)
        }
    }

    fun addXP(uuid: UUID) {
        if (!initialized) return
        if (!discordIdUUIDMap.containsKey(uuid)) {
            val resp = tzbot.queueRequest(TZRequest(UserIDFromUUIDData(uuid)))
            resp.whenComplete(BiConsumer { response: TZResponse?, err: Throwable? ->
                if (err != null && !response!!.isSuccessful) return@BiConsumer
                discordIdUUIDMap[uuid] = response!!.asLong

                val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
                buffer.putLong(response.asLong)
                sock.makeRequest(buffer.array())
            })
            return
        }
        val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
        buffer.putLong(discordIdUUIDMap[uuid]!!)

        sock.makeRequest(buffer.array())
    }
}
