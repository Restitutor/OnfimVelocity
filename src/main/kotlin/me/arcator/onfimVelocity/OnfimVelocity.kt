package me.arcator.onfimVelocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import java.util.*
import java.util.concurrent.TimeUnit
import me.arcator.onfimLib.SCTPIn
import me.arcator.onfimLib.UDPIn
import me.arcator.onfimLib.format.Chat
import me.arcator.onfimLib.format.GenericChat
import me.arcator.onfimLib.format.JoinQuit
import me.arcator.onfimLib.format.PrintableGeneric
import me.arcator.onfimLib.format.SJoin
import me.arcator.onfimLib.format.SQuit
import me.arcator.onfimLib.format.Switch
import me.arcator.onfimLib.out.Dispatcher
import me.arcator.onfimLib.utils.Unpacker
import net.kyori.adventure.text.Component
import org.slf4j.Logger

typealias UUIDSet = MutableSet<UUID>

@Plugin(id = "onfimvelocity", name = "OnfimVelocity", version = "1.8.0")
class OnfimVelocity
@Inject
constructor(private val server: ProxyServer, private val logger: Logger) {
    private val noRelayPlayers = mutableSetOf<UUID>()
    private val noImagePlayers = mutableSetOf<UUID>()
    private val cs = ChatSender(server, noImagePlayers, noRelayPlayers)

    private val unpacker = Unpacker(cs)
    private val uListener = UDPIn(unpacker::read)
    private val sListener = SCTPIn(unpacker::read)
    private val ds: Dispatcher =
        Dispatcher(
            { text ->
                // Debug logger.info(text)
            },
            uListener::port,
            sListener::port,
        )

    private val lastServer = HashMap<UUID, String>()

    init {
        unpacker.setOnHeartbeat(ds::getHeartbeat)
    }

    @Subscribe(priority = -99)
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("[OnfimVelocity] Starting up!")

        server.scheduler.buildTask(this) { -> ds.pingAll() }.repeat(30, TimeUnit.SECONDS).schedule()
        server.scheduler.buildTask(this, sListener).schedule()
        server.scheduler.buildTask(this, uListener).schedule()

        server.commandManager.register(
            server.commandManager.metaBuilder("toggleimage").plugin(this).build(),
            ToggleCommand(noImagePlayers, "image"),
        )
        server.commandManager.register(
            server.commandManager.metaBuilder("togglerelay").plugin(this).build(),
            ToggleCommand(noRelayPlayers, "chat"),
        )
        server.commandManager.register(
            server.commandManager.metaBuilder("globalrelay").plugin(this).build(),
            GlobalCommand(cs::toggle),
        )
    }

    private fun sendEvt(evt: GenericChat) {
        server.scheduler
            .buildTask(this) { ->
                // Outbound to other nodes
                ds.broadcast(evt)
                // Relay to self
                if (evt is PrintableGeneric) cs.say(evt)
            }
            .schedule()
    }

    @Subscribe(priority = 99)
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        logger.info("[OnfimVelocity] Shutting down!")
        ds.disable()
        sListener.disable()
        uListener.disable()
    }

    @Subscribe(priority = 99)
    fun onPlayerChat(event: PlayerChatEvent) {
        val msg: String = Chat.fromMessage(event.message)
        if (msg.isEmpty() || event.player.uniqueId in noRelayPlayers) return

        sendEvt(
            Chat(
                plaintext = msg,
                name = event.player.username,
                server = event.player.currentServer.orElse(null).serverInfo?.name ?: "Unknown",
                uuid = event.player.uniqueId,
            )
        )
    }

    @Subscribe(priority = 99)
    fun onConnect(event: ServerConnectedEvent) {
        // Switch
        val current = event.server.serverInfo.name
        lastServer[event.player.uniqueId] = current

        if (event.previousServer.isPresent) {
            val previous = event.previousServer.get().serverInfo.name

            sendEvt(SQuit(event.player.username, previous))
            sendEvt(Switch(name = event.player.username, server = current, fromServer = previous))
            sendEvt(SJoin(event.player.username, current))
        } else { // Join
            val player = event.player
            sendEvt(JoinQuit(name = player.username, server = current, type = "Join"))

            server.scheduler
                .buildTask(this) { ->
                    player.sendMessage(Component.text("Remember to read: /nrules | No theft."))
                }
                .delay(5L, TimeUnit.SECONDS)
                .schedule()
        }
    }

    @Subscribe(priority = 99)
    fun onDisconnect(event: DisconnectEvent) {
        val fallbackName = lastServer.remove(event.player.uniqueId)
        val server =
            event.player.currentServer.orElse(null)?.server?.serverInfo?.name
                ?: fallbackName
                ?: "Velocity"
        sendEvt(JoinQuit(name = event.player.username, server = server, type = "Quit"))
    }
}
