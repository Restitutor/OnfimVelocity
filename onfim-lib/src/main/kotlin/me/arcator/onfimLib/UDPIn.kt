package me.arcator.onfimLib

import java.net.BindException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import me.arcator.onfimLib.utils.SELF_PORT
import me.arcator.onfimLib.utils.bind_ip

@Suppress("unused")
class UDPIn(private val read: (String, ByteArray) -> Unit) : Runnable {
    private var active = true
    private val length = 4096

    val ds = DatagramSocket(null)

    override fun run() {
        while (active) {
            try {
                ds.bind(InetSocketAddress(bind_ip, SELF_PORT))
            } catch (e: BindException) {
                Thread.sleep(30000)
                continue
            }
            break
        }

        while (active) {
            try {
                val buf = ByteArray(length)
                val packet = DatagramPacket(buf, buf.size)
                ds.receive(packet)
                val received = packet.data.copyOf(packet.length)
                read("UDP", received)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        ds.close()
        println("Shutdown UDP In")
    }

    fun port() = SELF_PORT

    fun disable() {
        active = false
        ds.close()
    }
}
