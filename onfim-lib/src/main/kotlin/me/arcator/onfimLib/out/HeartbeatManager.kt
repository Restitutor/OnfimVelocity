package me.arcator.onfimLib.out

internal class HeartbeatManager(
    private val setMulticastHosts: (HostMap) -> Unit,
) {
    private val hosts = mutableListOf<HeartbeatStore>()

    fun addHeartbeat(nodeHost: String, nodeType: String, sctp: Port?) {
        if (sctp == null) return

        val hs = HeartbeatStore(sctp, nodeHost, nodeType)
        hosts.removeAll { h -> h.isMatch(hs) || h.isOld() }
        hosts.add(hs)

        associateHosts()
    }

    fun associateHosts() {
        val ports = HashMap<String, MutableList<Host>>()
        hosts.forEach { h ->
            val port = h.getPort()
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
            }
            setMulticastHosts(portsMapArray)
        }
    }
}
