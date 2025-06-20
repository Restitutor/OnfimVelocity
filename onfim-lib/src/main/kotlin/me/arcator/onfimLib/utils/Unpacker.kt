package me.arcator.onfimLib.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
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
) {
    private val objectMapper = ObjectMapper(MessagePackFactory()).registerKotlinModule()
    private val seenUuids = ArrayDeque<Int>()
    private var onHeartbeat: ((Heartbeat) -> Unit)? = null
    private lateinit var ds: DatagramSocket

    @Suppress("unused")
    fun initialize(socket: DatagramSocket, listener: ((Heartbeat) -> Unit)) {
        ds = socket
        onHeartbeat = listener
    }

    @Suppress("unused")
    fun read(protocol: String, serialized: ByteArray) {
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
            sendUdpMessage(serialized)
        }
    }

    fun sendUdpMessage(serialized: ByteArray) {
        // Relay it to the web console backend for visualizing
        val ipAddress = InetAddress.getByName(System.getenv("onfim_console_host"))
        try {
            // Bind socket
            synchronized(this) { ->
                // Destination address and port
                val packet = DatagramPacket(serialized, serialized.size, ipAddress, 2525)
                ds.send(packet)
                // logger("Message sent to $host:2525 ${serialized.size}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
