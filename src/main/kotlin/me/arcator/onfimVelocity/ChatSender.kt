package me.arcator.onfimVelocity

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import me.arcator.onfimLib.format.Chat
import me.arcator.onfimLib.format.ImageEvt
import me.arcator.onfimLib.format.PrintableGeneric
import me.arcator.onfimLib.interfaces.ChatSenderInterface

class ChatSender(
    private val server: ProxyServer,
    private val noImagePlayers: UUIDSet,
    private val noRelayPlayers: UUIDSet,
) : ChatSenderInterface {

    private var skipRelay = false

    private fun broadcastPlayers(): List<Player> {
        return server.allPlayers.filter { player -> !noRelayPlayers.contains(player.uniqueId) }
    }

    private fun shouldSkip() = (server.playerCount == 0 || skipRelay)

    fun toggle(v: Boolean) {
        skipRelay = v
    }

    override fun say(evt: Chat) {
        if (!evt.shouldRelay() || shouldSkip()) return

        val text = evt.getChatMessage()
        broadcastPlayers()
            // Avoid duplicates for different bungees to same server
            .filter { player -> player.currentServer.orElse(null)?.serverInfo?.name != evt.server }
            .forEach { player -> player.sendMessage(text) }
    }

    override fun say(evt: ImageEvt) {
        if (shouldSkip()) return
        for (comp in evt.getLines()) {
            broadcastPlayers()
                .filter { player -> !noImagePlayers.contains(player.uniqueId) }
                .forEach { player -> player.sendMessage(comp) }
        }
    }

    override fun say(evt: PrintableGeneric) {
        if (shouldSkip()) return
        val text = evt.getComponent()
        broadcastPlayers().forEach { player -> player.sendMessage(text) }
    }
}
