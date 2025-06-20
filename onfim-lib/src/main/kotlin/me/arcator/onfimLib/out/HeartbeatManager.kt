package me.arcator.onfimLib.out

internal class HeartbeatManager(
    private val setMulticastUDP: (HostMap) -> Unit,
    private val setMulticastSCTP: (HostMap) -> Unit,
) {
    private val hosts = mutableListOf<HeartbeatStore>()

    fun addHeartbeat(nodeHost: String, nodeType: String, udp: Port?, sctp: Port?) {
        if (udp == null && sctp == null) return

        val hs = HeartbeatStore(udp, sctp, nodeHost, nodeType)
        hosts.removeAll { h -> h.isMatch(hs) || h.isOld() }
        hosts.add(hs)

        associateHosts()
    }

    fun associateHosts() {
        Protocols.entries.forEach { protocol ->
            val ports = HashMap<String, MutableList<Host>>()
            hosts.forEach { h ->
                val port = h.getPort(protocol)
                if (port !== null) {
                    val host = Host(h.nodeHost, port)
                    if (!ports.containsKey(h.nodeType)) {
                        ports[h.nodeType] = mutableListOf()
                    }
                    ports[h.nodeType]!!.add(host)
                }
            }

            if (ports.isNotEmpty()) {
                val portsMapArray = HostMap()
                ports.forEach { (k, v) ->
                    portsMapArray[k] = v.toTypedArray()
                    // println("AH $protocol $k $v")
                }
                if (protocol == Protocols.UDP) setMulticastUDP(portsMapArray)
                else if (protocol == Protocols.SCTP) setMulticastSCTP(portsMapArray)
            }
        }
    }
}
