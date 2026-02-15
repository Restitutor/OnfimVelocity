package me.arcator.onfimLib.out

import com.sun.nio.sctp.MessageInfo
import com.sun.nio.sctp.SctpMultiChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.UnresolvedAddressException
import me.arcator.onfimLib.utils.SELF_PORT
import me.arcator.onfimLib.utils.hostname

typealias Host = Pair<String, Port>

typealias HostMap = HashMap<String, Array<Host>>

internal data class SocketManager(
    private val socket: SctpMultiChannel,
    private val log: (String) -> Unit,
) {
    companion object {
        val allHosts = mutableSetOf<Host>()

        init {
            arrayOf("jylina", "apollo", "icarus", "styx").forEach { h ->
                for (p in 2400..2403) {
                    // Exclude hardcoded self
                    if (h != hostname || p != SELF_PORT) allHosts.add(Pair(h, p))
                }
            }
        }
    }

    private val allHosts = SocketManager.allHosts.toMutableSet()
    private var multicastHosts = HostMap()

    fun setMulticastHosts(h: HostMap) {
        multicastHosts = h
    }

    fun unicast(packed: ByteArray, h: Host) {
        try {
            synchronized(socket) {
                socket.send(
                    ByteBuffer.wrap(packed),
                    MessageInfo.createOutgoing(InetSocketAddress(h.first, h.second), 0),
                )
            }
        } catch (e: UnresolvedAddressException) {
            System.err.println("Could not find ${h.first}")
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun multicast(packed: ByteArray, nodeType: String? = null) {
        getMulticastHosts(nodeType).forEach { h -> unicast(packed, h) }
    }

    fun broadcast(packed: ByteArray) {
        allHosts.forEach { h -> unicast(packed, h) }
    }

    private fun getMulticastHosts(nodeType: String?): Array<Host> {
        if (multicastHosts.isEmpty()) return allHosts.toTypedArray()
        if (multicastHosts.containsKey(nodeType)) {
            val ret = multicastHosts[nodeType]
            if (ret != null) return ret
        }

        val result = mutableListOf<Host>()
        multicastHosts.values.forEach { v -> result.addAll(v) }
        return result.toTypedArray()
    }
}
