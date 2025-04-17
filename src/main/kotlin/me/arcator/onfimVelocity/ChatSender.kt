package me.arcator.onfimVelocity

import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import me.arcator.onfimLib.format.Chat
import me.arcator.onfimLib.format.ImageEvt
import me.arcator.onfimLib.format.PrintableGeneric
import me.arcator.onfimLib.interfaces.ChatSenderInterface
import me.arcator.onfimLib.structs.ToggleSet

val noRelayPlayers = ToggleSet()
val noImagePlayers = ToggleSet()

class ChatSender(private val server: ProxyServer) : ChatSenderInterface {

    private fun broadcastPlayers(): List<Player> {
        return server.allPlayers.filter { player -> !noRelayPlayers.contains(player.uniqueId) }
    }

    override fun say(evt: Chat) {
        if (!evt.shouldRelay() || server.playerCount == 0) return

        val text = evt.getChatMessage()
        broadcastPlayers()
            // Avoid duplicates for different bungees to same server
            .filter { player -> player.currentServer.orElse(null)?.serverInfo?.name != evt.server }
            .forEach { player -> player.sendMessage(text) }
    }

    override fun say(evt: ImageEvt) {
        if (server.playerCount == 0) return
        for (comp in evt.getLines()) {
            broadcastPlayers()
                .filter { player -> !noImagePlayers.contains(player.uniqueId) }
                .forEach { player -> player.sendMessage(comp) }
        }
    }

    override fun say(evt: PrintableGeneric) {
        if (server.playerCount == 0) return
        val text = evt.getComponent()
        broadcastPlayers().forEach { player -> player.sendMessage(text) }
    }
}
