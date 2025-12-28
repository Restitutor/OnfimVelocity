package me.arcator.onfimVelocity

import com.m3z0id.tzbot4j.TZBot4J
import com.velocitypowered.api.proxy.ProxyServer
import me.arcator.onfimLib.format.Chat
import me.arcator.onfimLib.format.ImageEvt
import me.arcator.onfimLib.format.PlayerMoveInterface
import me.arcator.onfimLib.format.ServerMessage
import me.arcator.onfimLib.interfaces.ChatSenderInterface

class ChatSender(
    private val server: ProxyServer,
    private val noImagePlayers: UUIDSet,
    private val noRelayPlayers: UUIDSet,
    private val tzBot: TZBot4J
) : ChatSenderInterface {
    var skipRelay = false

    private fun broadcastPlayers() =
        server.allPlayers.filter { player -> !noRelayPlayers.contains(player.uniqueId) }

    private fun shouldSkip() = (server.playerCount == 0 || skipRelay)

    fun toggle(v: Boolean) {
        skipRelay = v
    }

    override fun say(evt: Chat) {
        if (!evt.shouldRelay() || shouldSkip()) return

        val text = evt.getChatMessage()
        val filteredPlayers = broadcastPlayers()
            // Avoid duplicates for different bungees to same server
            .filter { player ->
                player.currentServer
                    .orElse(null)
                    ?.serverInfo
                    ?.name != evt.server.name
            }

        filteredPlayers.forEach { player ->
            player.sendMessage(tzBot.tzManager.adjustForPlayer(text, player.uniqueId) ?: text)
        }

    }

    override fun say(evt: ImageEvt) {
        if (shouldSkip()) return
        for (comp in evt.getLines()) {
            broadcastPlayers()
                .filter { player -> !noImagePlayers.contains(player.uniqueId) }
                .forEach { player -> player.sendMessage(comp) }
        }
    }

    override fun say(evt: ServerMessage) {
        if (shouldSkip()) return
        val text = evt.getComponent()
        broadcastPlayers().forEach { player -> player.sendMessage(text) }
    }

    override fun say(evt: PlayerMoveInterface) {
        if (shouldSkip()) return
        val text = evt.getComponent()
        broadcastPlayers().forEach { player -> player.sendMessage(text) }
    }
}
