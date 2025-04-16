package me.arcator.onfimVelocity

import com.google.inject.Inject
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.scheduler.ScheduledTask
import java.util.concurrent.TimeUnit
import me.arcator.onfimLib.format.Chat
import me.arcator.onfimLib.format.GenericChat
import me.arcator.onfimLib.format.JoinQuit
import me.arcator.onfimLib.format.PrintableGeneric
import me.arcator.onfimLib.format.SJoin
import me.arcator.onfimLib.format.SQuit
import me.arcator.onfimLib.format.Switch
import me.arcator.onfimLib.out.Dispatcher
import me.arcator.onfimLib.sIn
import me.arcator.onfimLib.uIn
import org.slf4j.Logger

@Plugin(
    id = "onfimvelocity", name = "OnfimVelocity", version = "1.7.0",
)
class OnfimVelocity @Inject constructor(val server: ProxyServer, val logger: Logger) {
    private val cs = ChatSender(server)
    private var sListener = sIn(cs)
    private var uListener = uIn(cs)
    private val ds = Dispatcher { text ->
        // Debug logger.info(text)
    }
    var dScheduler: ScheduledTask? = null;
    var sScheduler: ScheduledTask? = null;
    var uScheduler: ScheduledTask? = null;

    @Subscribe
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("[OnfimVelocity] Starting up!")

        dScheduler = server.scheduler.buildTask(this) { ->
            ds.pingAll()
        }.repeat(30, TimeUnit.SECONDS).schedule()
        sScheduler = server.scheduler.buildTask(this, sListener).schedule()
        uScheduler = server.scheduler.buildTask(this, uListener).schedule()
    }

    private fun sendEvt(evt: GenericChat) {
        server.scheduler.buildTask(this) { ->
            // Outbound to other nodes
            ds.broadcast(evt)
            // Relay to self
            if (evt is PrintableGeneric) cs.say(evt)
        }.schedule()
    }

    @Subscribe
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        logger.info("[OnfimVelocity] Shutting down!")
        ds.disable()
        sListener.disable()
        uListener.disable()
    }

    @Subscribe(priority = 1, order = PostOrder.CUSTOM)
    fun onPlayerChat(event: PlayerChatEvent) {
        val msg: String = Chat.fromMessage(event.message)
        if (msg.isEmpty()) return

        sendEvt(
            Chat(
                plaintext = msg,
                name = event.player.username,
                server = event.player.currentServer.orElse(null).serverInfo?.name ?: "Unknown",
                uuid = event.player.uniqueId,
            ),
        )
    }

    @Subscribe
    fun onConnect(event: ServerConnectedEvent) {
        // Switch
        val current = event.server.serverInfo.name
        if (event.previousServer.isPresent) {
            val previous = event.previousServer.get().serverInfo.name

            sendEvt(SQuit(event.player.username, previous))
            sendEvt(Switch(name = event.player.username, server = current, fromServer = previous))
            sendEvt(SJoin(event.player.username, current))
        } else { // Join
            val player = event.player
            sendEvt(JoinQuit(name = player.username, server = current, type = "Join"))
        }
    }

    @Subscribe
    fun onDisconnect(event: DisconnectEvent) {
        val server = event.player.currentServer.orElse(null)?.server?.serverInfo?.name ?: "Velocity"
        sendEvt(JoinQuit(name = event.player.username, server = server, type = "Quit"))
    }
}
