package me.arcator.onfimVelocity

import com.velocitypowered.api.proxy.ProxyServer
import me.arcator.onfimLib.format.Chat
import me.arcator.onfimLib.format.ImageEvt
import me.arcator.onfimLib.format.PlayerMoveInterface
import me.arcator.onfimLib.format.ServerMessage
import me.arcator.onfimLib.interfaces.ChatSenderInterface
import me.arcator.onfimVelocity.timezone.Timezone
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextReplacementConfig

class ChatSender(
    private val server: ProxyServer,
    private val noImagePlayers: UUIDSet,
    private val noRelayPlayers: UUIDSet,
    private val tz: Timezone
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

        val regex = "<t:(\\d{1,13})(?::([fFdDtTrR]))?>".toRegex()
        val matches = regex.findAll(evt.plaintext).toList()

        if (matches.isNotEmpty()) {
            val substrTimestampMode = matches.map { match ->
                val substr = match.value
                var timestamp: Long? = match.groups[1]!!.value.toLong()
                val mode = match.groups[2]?.value?.get(0) ?: 'f'

                // If timestamp is bigger than discord max timestamp
                if (timestamp!! > 8640000000000L) timestamp = null

                Triple(substr, timestamp, mode)
            }

            filteredPlayers.forEach { player ->
                var chatMessage = evt.getChatMessage()

                substrTimestampMode.forEach start@{ triple ->
                    val replacement =
                        tz.getTime(player.uniqueId, triple.second ?: return@start, triple.third)

                    val config = TextReplacementConfig.builder()
                        .match(triple.first)
                        .replacement(Component.text(replacement))
                        .build()

                    chatMessage = chatMessage.replaceText(config)
                }

                player.sendMessage(chatMessage)
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
