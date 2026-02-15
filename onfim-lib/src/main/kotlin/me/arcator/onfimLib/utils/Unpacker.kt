package me.arcator.onfimLib.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sun.nio.sctp.MessageInfo
import com.sun.nio.sctp.SctpMultiChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.util.*
import me.arcator.onfimLib.format.Chat
import me.arcator.onfimLib.format.Heartbeat
import me.arcator.onfimLib.format.ImageEvt
import me.arcator.onfimLib.format.JoinQuit
import me.arcator.onfimLib.format.SerializedEvent
import me.arcator.onfimLib.format.ServerMessage
import me.arcator.onfimLib.format.Switch
import me.arcator.onfimLib.interfaces.ChatSenderInterface
import org.msgpack.jackson.dataformat.MessagePackFactory

class Unpacker(
    private val chatSender: ChatSenderInterface,
    private val logger: ((String) -> Unit),
    private val sctpSocket: SctpMultiChannel,
) {
    private val objectMapper = ObjectMapper(MessagePackFactory()).registerKotlinModule()
    private val seenUuids = ArrayDeque<Int>()
    private var onHeartbeat: ((Heartbeat) -> Unit)? = null
    private val consoleAddress = InetSocketAddress("apollo", 2525)

    @Suppress("unused")
    fun initialize(listener: ((Heartbeat) -> Unit)) {
        onHeartbeat = listener
    }

    @Suppress("unused")
    fun read(serialized: ByteArray) {
        val meta = objectMapper.readValue(serialized, SerializedEvent::class.java)
        val evtType = meta.type

        synchronized(this) {
            if (meta.id in seenUuids) return
            if (evtType == "Chat") {
                val evt: Chat = objectMapper.readValue(serialized, Chat::class.java)
                if (evt.shouldShow()) chatSender.say(evt)
            } else if (evtType in arrayOf("Join", "Quit")) {
                val evt: JoinQuit = objectMapper.readValue(serialized, JoinQuit::class.java)
                chatSender.say(evt)
            } else if (evtType == "Switch") {
                val evt: Switch = objectMapper.readValue(serialized, Switch::class.java)
                chatSender.say(evt)
            } else if (evtType == "Heartbeat") {
                onHeartbeat?.invoke(objectMapper.readValue(serialized, Heartbeat::class.java))
            } else if (evtType == "Image") {
                chatSender.say(objectMapper.readValue(serialized, ImageEvt::class.java))
            } else if (evtType == "Server Message") {
                chatSender.say(objectMapper.readValue(serialized, ServerMessage::class.java))
            }

            seenUuids.addLast(meta.id)
            while (seenUuids.size > 50) seenUuids.removeFirst()
            sendMessage(serialized)
        }
    }

    fun sendMessage(serialized: ByteArray) {
        // Relay it to the web console backend for visualizing
        try {
            synchronized(sctpSocket) {
                sctpSocket.send(
                    ByteBuffer.wrap(serialized),
                    MessageInfo.createOutgoing(consoleAddress, 0),
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
