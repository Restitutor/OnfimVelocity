package me.arcator.onfimLib.out

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.sun.nio.sctp.SctpMultiChannel
import java.net.DatagramSocket
import me.arcator.onfimLib.format.Heartbeat
import me.arcator.onfimLib.format.SerializedEvent
import me.arcator.onfimLib.utils.hostnameTitle
import org.msgpack.jackson.dataformat.MessagePackFactory

class Dispatcher(
    private val logger: ((String) -> Unit),
    private val getUdpInPort: () -> Port,
    private val getSctpInPort: () -> Port,
    sctpSocket: SctpMultiChannel,
    udpSocket: DatagramSocket
) {
    private val objectMapper = ObjectMapper(MessagePackFactory()).registerKotlinModule()

    private val uOut = SocketManager(UDPOut(udpSocket), logger)
    private val sOut = SocketManager(SCTPOut(sctpSocket), logger)
    private val hm = HeartbeatManager(uOut::setMulticastHosts, sOut::setMulticastHosts)
    private val rOut = RelayOut(logger, sOut::unicast)

    fun broadcast(evt: SerializedEvent) {
        val bytes = objectMapper.writeValueAsBytes(evt)
        logger("[Onfim] Send ${evt.type}")
        uOut.multicast(bytes, "JS")
        sOut.multicast(bytes)
        rOut.sendEvent(evt)
    }

    fun broadcast(text: String) {
        rOut.sendText("Velocity$hostnameTitle", text)
    }

    fun getHeartbeat(h: Heartbeat) {
        if (h.udp == null && h.sctp == null) return

        hm.addHeartbeat(h.node.host, h.node.type, h.udp, h.sctp)
    }

    // Repeatable
    fun pingAll() {
        hm.associateHosts()
        val h = Heartbeat(udp = getUdpInPort(), sctp = getSctpInPort())
        if (h.udp != null || h.sctp != null) {
            uOut.broadcast(objectMapper.writeValueAsBytes(h))
        }
    }
}
