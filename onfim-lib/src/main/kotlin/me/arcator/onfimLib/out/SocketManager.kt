package me.arcator.onfimLib.out

import java.net.InetSocketAddress
import me.arcator.onfimLib.utils.SELF_PORT
import me.arcator.onfimLib.utils.hostname

typealias Host = Pair<String, Port>

typealias HostMap = HashMap<String, Array<Host>>

internal data class SocketManager(
    private val unicastImpl: UnicastInterface,
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
            // log("Sent ${unicastImpl.type} $h")
            unicastImpl.send(packed, InetSocketAddress(h.first, h.second))
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
