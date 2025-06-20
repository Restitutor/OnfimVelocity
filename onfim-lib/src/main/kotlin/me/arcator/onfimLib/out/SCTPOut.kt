package me.arcator.onfimLib.out

import com.sun.nio.sctp.MessageInfo
import com.sun.nio.sctp.SctpMultiChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.UnresolvedAddressException

internal class SCTPOut(private val socket: SctpMultiChannel) : UnicastInterface {
    override val type = "SCTP"
    override fun send(message: ByteArray, host: InetSocketAddress) {
        synchronized(socket) {
            try {
                socket.send(ByteBuffer.wrap(message), MessageInfo.createOutgoing(host, 0))
            } catch (e: UnresolvedAddressException) {
                System.err.println("Could not find ${host.hostString}")
                e.printStackTrace()
            }
        }
    }
}
