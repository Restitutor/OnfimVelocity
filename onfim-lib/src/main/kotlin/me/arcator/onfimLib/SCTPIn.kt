package me.arcator.onfimLib

import com.sun.nio.sctp.AbstractNotificationHandler
import com.sun.nio.sctp.SctpMultiChannel
import com.sun.nio.sctp.SctpStandardSocketOptions
import java.io.PrintStream
import java.net.BindException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.SelectionKey
import me.arcator.onfimLib.utils.SELF_PORT
import me.arcator.onfimLib.utils.bind_ip

@Suppress("unused")
class SCTPIn(private val read: (ByteArray) -> Unit) : Runnable {
    private var active = true
    private val length = 30000
    private val assocHandler = object : AbstractNotificationHandler<PrintStream>() {}
    val ds: SctpMultiChannel = SctpMultiChannel.open()

    override fun run() {
        while (active) {
            try {
                ds.bind(InetSocketAddress(bind_ip, SELF_PORT))
                ds.setOption(SctpStandardSocketOptions.SCTP_NODELAY, true, null)
            } catch (e: BindException) {
                Thread.sleep(30000)
                continue
            }
            break
        }

        ds.configureBlocking(false)
        val selector = ds.provider().openSelector()
        ds.register(selector, SelectionKey.OP_READ)

        var closedCount = 0
        while (active && closedCount < 10) {
            try {
                if (selector.select(1000) == 0) continue
                selector.selectedKeys().clear()

                val buf = ByteBuffer.allocateDirect(length)
                ds.receive(buf, System.out, assocHandler)

                val actualLength = buf.position()
                buf.rewind()
                val arr = ByteArray(actualLength)
                buf.get(arr, 0, actualLength)
                read(arr)
            } catch (e: ClosedChannelException) {
                closedCount += 1
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        selector.close()
        ds.close()
        println("Shutdown sctp In")
    }

    fun port() = SELF_PORT

    fun disable() {
        active = false
    }
}
