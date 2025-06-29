package me.arcator.onfimLib.out

import java.nio.charset.StandardCharsets
import me.arcator.onfimLib.format.Chat
import me.arcator.onfimLib.format.JoinQuit
import me.arcator.onfimLib.format.SerializedEvent
import me.arcator.onfimLib.format.ServerMessage
import me.arcator.onfimLib.format.Switch
import me.arcator.onfimLib.utils.RELAY_PORT
import me.arcator.onfimLib.utils.hostnameTitle

class RelayOut(
    private val logger: ((String) -> Unit),
    private val unicast: ((ByteArray, Host) -> Unit),
) {
    fun sendText(server: String, text: String) = send("Server Message|${server}|${text}")

    fun sendEvent(event: SerializedEvent) {
        if (event.type === "SJoin" || event.type === "SQuit") return

        send(
            when (event) {
                is Chat -> {
                    "Chat|${event.server.name}|${event.user.name}|${event.plaintext}"
                }

                is ServerMessage -> {
                    "Server Message|${event.server}|${event.text}"
                }

                is Switch -> {
                    val text =
                        "${event.username} ($hostnameTitle) moved from ${event.fromServer}->${event.server.name}"
                    return sendText(event.server.name, text)
                }

                is JoinQuit -> {
                    when (event.type) {
                        "Join" -> "Join|${event.server.name}|${event.username}"
                        "Quit" -> "Quit|${event.server.name}|${event.username}"
                        else -> return
                    }
                }

                else -> return
            },
        )
    }

    private fun send(text: String) {
        unicast(text.toByteArray(StandardCharsets.UTF_8), Pair("relay", RELAY_PORT))
    }
}
