package me.arcator.onfimLib.out

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

internal class UDPOut(private val socket: DatagramSocket) : UnicastInterface {
    // Operate in blocking mode such that a socket only sends data one at a time
    override val type = "UDP"
    override fun send(message: ByteArray, host: InetSocketAddress) {
        synchronized(socket) { socket.send(DatagramPacket(message, message.size, host)) }
    }
}
