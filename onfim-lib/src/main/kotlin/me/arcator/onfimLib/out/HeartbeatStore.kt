package me.arcator.onfimLib.out

typealias Port = Int

data class HeartbeatStore(
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

    fun getPort(): Port? = sctp
}
