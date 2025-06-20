package me.arcator.onfimLib.out

enum class Protocols {
    UDP,
    SCTP,
}

typealias Port = Int

data class HeartbeatStore(
    private val udp: Port?,
    private val sctp: Port?,
    val nodeHost: String,
    val nodeType: String,
) {

    private val lastPing: Int = nowSeconds()

    companion object {
        fun nowSeconds() = (System.currentTimeMillis() / 1000).toInt()
    }

    fun isMatch(hs: HeartbeatStore) = nodeHost == hs.nodeHost && nodeType == hs.nodeType

    fun isOld() = lastPing < nowSeconds() - 120 // Two minutes

    fun getPort(prop: Protocols): Port? {
        if (prop == Protocols.UDP) return udp
        if (prop == Protocols.SCTP) return sctp
        return null
    }
}
