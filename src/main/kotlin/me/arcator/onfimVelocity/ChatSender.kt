package me.arcator.onfimVelocity

import com.velocitypowered.api.proxy.ProxyServer
import me.arcator.onfimLib.format.Chat
import me.arcator.onfimLib.format.ImageEvt
import me.arcator.onfimLib.format.PlayerMoveInterface
import me.arcator.onfimLib.format.ServerMessage
import me.arcator.onfimLib.interfaces.ChatSenderInterface
import me.arcator.onfimVelocity.timezone.Timezone
import me.arcator.onfimVelocity.timezone.Timezone.extractTimestamp
import net.kyori.adventure.text.Component

class ChatSender(
    private val server: ProxyServer,
    private val noImagePlayers: UUIDSet,
    private val noRelayPlayers: UUIDSet,
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

        val plainText = String(evt.plaintext.toCharArray()) // Copy
        val regex = "<t:\\d{1,20}:?[fFDdtTR]?>".toRegex()
        val matches = regex.findAll(plainText).map { it.value }.toList()

        if(matches.isNotEmpty()) {
            filteredPlayers.forEach { player ->
                run {
                    var textCpy = ""
                    matches.forEach { match ->
                        run {
                            var mode = match.toCharArray()[match.length - 2]
                            if(mode.isDigit()) {
                                mode = '?'
                            }
                            val timestamp = extractTimestamp(match)
                            var replacement = Timezone.getTime(player.uniqueId, timestamp, mode)

                            if(replacement.startsWith(".")) {
                                replacement = replacement.substring(1)
                                replacement += " (set your timezone with /mytimezone set <timezone> <your mc username in lowercase> to get personalized time)"
                            }

                            textCpy = plainText.replace(match, replacement)
                        }
                    }
                    player.sendMessage(Component.text(textCpy).style(text.style()))
                }
            }
        } else {
            filteredPlayers.forEach { player ->
                player.sendMessage(text)
            }
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
