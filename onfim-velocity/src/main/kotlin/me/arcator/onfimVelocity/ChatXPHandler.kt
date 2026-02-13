package me.arcator.onfimVelocity

import com.m3z0id.tzbot4j.TZBot4J
import com.m3z0id.tzbot4j.network.UDPSocket
import com.m3z0id.tzbot4j.tzLib.net.TZRequest
import com.m3z0id.tzbot4j.tzLib.net.TZResponse
import com.m3z0id.tzbot4j.tzLib.net.c2s.UserIDFromUUIDData
import java.net.SocketException
import java.net.UnknownHostException
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.function.BiConsumer

class ChatXPHandler(tzbot: TZBot4J) {
    private val sock: UDPSocket
    private val tzbot: TZBot4J
    private val discordIdUUIDMap: MutableMap<UUID, Long>
    private val notLinked: MutableSet<UUID> = mutableSetOf()
    private val invalidateScheduler: ScheduledExecutorService
    private var initialized = false

    init {
        try {
            sock = UDPSocket("jylina", 8890)
            discordIdUUIDMap = mutableMapOf()
            this.tzbot = tzbot

            invalidateScheduler = Executors.newScheduledThreadPool(1)

            initialized = true
        } catch (e: SocketException) {
            initialized = false
            throw RuntimeException(e)
        } catch (e: UnknownHostException) {
            initialized = false
            throw RuntimeException(e)
        }
    }

    fun close() {
        invalidateScheduler.close()
        sock.close()
    }

    fun addUUID(uuid: UUID, discordId: Long) {
        discordIdUUIDMap[uuid] = discordId
    }

    private fun makeAddXPRequest(discordId: Long) {
        val buffer = ByteBuffer.allocate(Long.SIZE_BYTES)
        buffer.putLong(discordId)
        sock.makeRequest(buffer.array())
    }

    private fun fetchDiscordId(uuid: UUID) {
        val resp = tzbot.queueRequest(TZRequest(UserIDFromUUIDData(uuid)))
        resp.whenComplete(BiConsumer { response: TZResponse?, err: Throwable? ->
            if ((err != null && !response!!.isSuccessful) || response?.asLong == 0L) {
                notLinked.add(uuid)
                return@BiConsumer
            }

            discordIdUUIDMap[uuid] = response!!.asLong
        })
    }

    fun addXP(uuid: UUID) {
        if (!initialized || uuid in notLinked) return
        if (!discordIdUUIDMap.containsKey(uuid)) fetchDiscordId(uuid)
        makeAddXPRequest(discordIdUUIDMap[uuid] ?: return)
    }

    fun deleteEntry(uuid: UUID?) {
        discordIdUUIDMap.remove(uuid ?: return)
    }

    fun deleteFromNotLinked(uuid: UUID) {
        notLinked.remove(uuid)
    }

    fun startInvalidating() {
        invalidateScheduler.scheduleAtFixedRate({ discordIdUUIDMap.clear(); notLinked.clear() }, 10L, 10L, TimeUnit.MINUTES)
    }

    fun isInvalidatorRunning(): Boolean {
        return !invalidateScheduler.isTerminated
    }

    fun stopInvalidating() {
        invalidateScheduler.close()
    }
}
