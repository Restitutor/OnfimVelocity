package me.arcator.onfimLib.out

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sun.nio.sctp.SctpMultiChannel
import me.arcator.onfimLib.format.Heartbeat
import me.arcator.onfimLib.format.SerializedEvent
import me.arcator.onfimLib.utils.hostnameTitle
import org.msgpack.jackson.dataformat.MessagePackFactory

class Dispatcher(
    private val logger: ((String) -> Unit),
    private val getSctpInPort: () -> Port,
    sctpSocket: SctpMultiChannel,
) {
    private val objectMapper = ObjectMapper(MessagePackFactory()).registerKotlinModule()

    private val sOut = SocketManager(sctpSocket, logger)
    private val hm = HeartbeatManager(sOut::setMulticastHosts)
    private val rOut = RelayOut(logger, sOut::unicast)

    fun broadcast(evt: SerializedEvent) {
        val bytes = objectMapper.writeValueAsBytes(evt)
        logger("[Onfim] Send ${evt.type}")
        sOut.multicast(bytes)
        rOut.sendEvent(evt)
    }

    fun broadcast(text: String) {
        rOut.sendText("Velocity$hostnameTitle", text)
    }

    fun getHeartbeat(h: Heartbeat) {
        if (h.sctp == null) return

        hm.addHeartbeat(h.node.host, h.node.type, h.sctp)
    }

    // Repeatable
    fun pingAll() {
        hm.associateHosts()
        val h = Heartbeat(sctp = getSctpInPort())
        if (h.sctp != null) {
            sOut.broadcast(objectMapper.writeValueAsBytes(h))
        }
    }
}
