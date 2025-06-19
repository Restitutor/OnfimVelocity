package me.arcator.onfimVelocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.command.CommandExecuteEvent
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import java.nio.file.Path
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit
import me.arcator.onfimLib.SCTPIn
import me.arcator.onfimLib.UDPIn
import me.arcator.onfimLib.format.Chat
import me.arcator.onfimLib.format.ChatUser
import me.arcator.onfimLib.format.EventLocation
import me.arcator.onfimLib.format.PlayerMoveInterface
import me.arcator.onfimLib.format.RELAY_CMDS
import me.arcator.onfimLib.format.SJoin
import me.arcator.onfimLib.format.SQuit
import me.arcator.onfimLib.format.SerializedEvent
import me.arcator.onfimLib.format.ServerMessage
import me.arcator.onfimLib.format.makeJoinQuit
import me.arcator.onfimLib.format.makeSwitch
import me.arcator.onfimLib.out.Dispatcher
import me.arcator.onfimLib.utils.Unpacker
import me.arcator.onfimVelocity.timezone.TCPSock
import net.kyori.adventure.text.Component
import org.slf4j.Logger

typealias UUIDSet = MutableSet<UUID>

@Plugin(id = "onfimvelocity", name = "OnfimVelocity", version = "1.8.1")
class OnfimVelocity
@Inject
constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path,
) {
    private val noRelay = PersistSet(dataDirectory.resolve("no-relay.txt"))
    private val noImage = PersistSet(dataDirectory.resolve("no-image.txt"))
    private val cs = ChatSender(server, noImage.players, noRelay.players)

    private val unpacker = Unpacker(cs, logger::info)
    private val uListener = UDPIn(unpacker::read)
    private val sListener = SCTPIn(unpacker::read)
    private val ds: Dispatcher = Dispatcher(logger::info, uListener::port, sListener::port, sListener.ds, uListener.ds)

    private val lastServer = HashMap<UUID, String>()

    init {
        unpacker.initialize(uListener.ds, ds::getHeartbeat)
    }

    companion object {
        @JvmStatic
        val playerTimezones: MutableMap<UUID, ZoneId> = mutableMapOf()
    }

    @Subscribe(priority = -99)
    fun onProxyInitialization(event: ProxyInitializeEvent) {
        logger.info("[OnfimVelocity] Starting up!")

        server.scheduler.buildTask(this) { -> ds.pingAll() }.repeat(30, TimeUnit.SECONDS).schedule()
        server.scheduler.buildTask(this, sListener).schedule()
        server.scheduler.buildTask(this, uListener).schedule()

        server.commandManager.register(
            server.commandManager.metaBuilder("toggleimage").plugin(this).build(),
            ToggleCommand(server::getPlayer, noImage.players, "image"),
        )
        server.commandManager.register(
            server.commandManager.metaBuilder("togglerelay").plugin(this).build(),
            ToggleCommand(server::getPlayer, noRelay.players, "chat"),
        )
        server.commandManager.register(
            server.commandManager.metaBuilder("globalrelay").plugin(this).build(),
            GlobalCommand {
                value: Boolean, text: String ->
                cs.toggle(value)
                sendEvt(
                    ServerMessage(
                        text=text
                    )
                )
        })
    }

    private fun sendEvt(evt: SerializedEvent) {
        server.scheduler
            .buildTask(this) { ->
                // Outbound to other nodes
                ds.broadcast(evt)
                // Relay to self
                if (evt is ServerMessage) {
                    cs.say(evt)
                } else if (evt is PlayerMoveInterface && evt.type !in hashSetOf("SJoin", "SQuit")) {
                    cs.say(evt)
                }
            }
            .schedule()
    }

    @Subscribe(priority = 99)
    fun onProxyShutdown(event: ProxyShutdownEvent) {
        logger.info("[OnfimVelocity] Shutting down!")
        sListener.disable()
        uListener.disable()
        noRelay.save()
        noImage.save()
    }

    private fun sendChat(rawMsg: String, player: Player) {
        val msg: String = Chat.fromMessage(rawMsg)
        if (msg.isEmpty() || cs.skipRelay || player.uniqueId in noRelay.players) return

        sendEvt(
            Chat(
                plaintext = msg,
                user = ChatUser(player.username, uuid = player.uniqueId),
                server =
                    EventLocation(
                        name = player.currentServer.orElse(null).serverInfo?.name ?: "Unknown"
                    ),
            )
        )
    }

    @Subscribe(priority = 99)
    fun onPlayerChat(event: PlayerChatEvent) {
        if (!event.player.isOnlineMode) return
        sendChat(event.message, event.player)
    }

    @Subscribe(priority = -99)
    fun onCommand(event: CommandExecuteEvent) {
        val player = event.commandSource as? Player ?: return
        if (!player.isOnlineMode) return

        // Extract base command without arguments
        val command = event.command.substringBefore(" ")

        // Relay if publicly visible. Prepend / for special parsing.
        if (command in RELAY_CMDS) sendChat("/${event.command}", player)
    }

    @Subscribe(priority = 99)
    fun onConnect(event: ServerConnectedEvent) {
        val current = event.server.serverInfo.name
        lastServer[event.player.uniqueId] = current
        if (!event.player.isOnlineMode) return

        val name = event.player.username
        if (event.previousServer.isPresent) {
            val previous = event.previousServer.get().serverInfo.name

            sendEvt(SQuit(username = name, previous))
            sendEvt(makeSwitch(username = name, serverName = current, fromServer = previous))
            sendEvt(SJoin(username = name, current))
        } else { // Join
            val player = event.player
            sendEvt(makeJoinQuit(username = name, serverName = current, type = "Join"))

            // Disable Xaero Minimaps cave view
            var greet = "Remember to read: /nrules | No theft.§x§a§e§r§o§w§m§n§e§t§h§e§r§i§s§f§a§i§r"
            if (event.player.uniqueId in noRelay.players) {
                greet += "\nYour Discord relay is off."
            } else if (event.player.uniqueId in noImage.players) {
                greet += "\nYour Image relay is off."
            }
            server.scheduler
                .buildTask(this) { -> player.sendMessage(Component.text(greet)) }
                .delay(5L, TimeUnit.SECONDS)
                .schedule()
        }
    }

    @Subscribe
    fun onPlayerProxyConnect(event: LoginEvent) {
        val player = event.player ?: return
        val username = player.username ?: return
        val ip: String? = player.remoteAddress?.address?.hostAddress

        if(player.uniqueId !in playerTimezones) {
            val timezone = TCPSock.sendAliasTZRequest(username) ?: TCPSock.sendIPTZRequest(ip ?: return) ?: return
            playerTimezones[player.uniqueId] = ZoneId.of(timezone)
        }
    }

    @Subscribe(priority = 99)
    fun onDisconnect(event: DisconnectEvent) {
        if (!event.player.isOnlineMode) return
        val fallbackName = lastServer.remove(event.player.uniqueId)

        // Remove timezone
        playerTimezones.remove(event.player.uniqueId)

        val server =
            event.player.currentServer.orElse(null)?.server?.serverInfo?.name ?: fallbackName

        // Unsuccessful login. Don't print.
        if (server == null) return

        val name = event.player.username
        sendEvt(makeJoinQuit(username = name, serverName = server, type = "Quit"))
    }
}
