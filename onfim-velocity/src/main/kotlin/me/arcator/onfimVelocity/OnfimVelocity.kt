package me.arcator.onfimVelocity

import com.google.inject.Inject
import com.m3z0id.tzbot4j.TZBot4J
import com.m3z0id.tzbot4j.config.Config
import com.m3z0id.tzbot4j.config.subclasses.TZFlag
import com.velocitypowered.api.event.ResultedEvent
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.command.CommandExecuteEvent
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.LoginEvent
import com.velocitypowered.api.event.player.PlayerChatEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyPingEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.event.query.ProxyQueryEvent
import com.velocitypowered.api.network.ProtocolVersion
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.annotation.DataDirectory
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.QueryResponse
import java.nio.file.Path
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Predicate
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
import me.arcator.onfimLib.format.cleanName
import me.arcator.onfimLib.format.makeJoinQuit
import me.arcator.onfimLib.format.makeSwitch
import me.arcator.onfimLib.out.Dispatcher
import me.arcator.onfimLib.utils.Unpacker
import me.arcator.onfimVelocity.chatXP.ChatXPHandler
import me.arcator.onfimVelocity.timezone.TimezoneCommand
import net.kyori.adventure.text.Component
import org.slf4j.Logger

typealias UUIDSet = MutableSet<UUID>

@Plugin(id = "onfimvelocity", name = "OnfimVelocity", version = "1.8.3")
class OnfimVelocity
@Inject
constructor(
    private val server: ProxyServer,
    private val logger: Logger,
    @DataDirectory private val dataDirectory: Path,
) {
    private val tzBot = TZBot4J.init(logger, Config.get(dataDirectory.toFile()), TZFlag.AES, TZFlag.MSGPACK)
    private val isOnlinePredicate = Predicate<UUID> { uuid -> server.getPlayer(uuid).isPresent }
    private val chatXPHandler = ChatXPHandler(tzBot)
    private val noRelay = PersistSet(dataDirectory.resolve("no-relay.txt"))
    private val noImage = PersistSet(dataDirectory.resolve("no-image.txt"))
    private val cs = ChatSender(server, noImage.players, noRelay.players, tzBot)

    private val unpacker = Unpacker(cs, logger::info)
    private val uListener = UDPIn(unpacker::read)
    private val sListener = SCTPIn(unpacker::read)
    private val ds: Dispatcher =
        Dispatcher(logger::info, uListener::port, sListener::port, sListener.ds, uListener.ds)

    private val lastServer = HashMap<UUID, String>()

    init {
        unpacker.initialize(uListener.ds, ds::getHeartbeat)
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
            server.commandManager.metaBuilder("timezone").aliases("tz").plugin(this).build(),
            TimezoneCommand(this, server, tzBot).createTimezoneCommand(),
        )
        server.commandManager.register(
            server.commandManager.metaBuilder("globalrelay").plugin(this).build(),
            GlobalCommand { value: Boolean, text: String ->
                cs.toggle(value)
                sendEvt(
                    ServerMessage(
                        text = text,
                    ),
                )
            },
        )
    }

    private fun sendEvt(evt: SerializedEvent) {
        server.scheduler
            .buildTask(this) { ->
                // Outbound to other nodes
                ds.broadcast(evt)
                // Relay to self
                if (evt is ServerMessage) {
                    cs.say(evt)
                } else if (evt is PlayerMoveInterface &&
                    evt.type !in hashSetOf("SJoin", "SQuit")
                ) {
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
        tzBot.close()
    }

    private fun sendChat(rawMsg: String, player: Player) {
        if (rawMsg.isEmpty() || cs.skipRelay || player.uniqueId in noRelay.players) return
        val msg: String = Chat.fromMessage(rawMsg)
        sendEvt(
            Chat(
                plaintext = msg,
                user = ChatUser(cleanName(player.username), uuid = player.uniqueId),
                server =
                    EventLocation(
                        name = player.currentServer.orElse(null).serverInfo?.name
                            ?: "Unknown",
                    ),
            ),
        )
    }

    @Subscribe(priority = 99)
    fun onPlayerChat(event: PlayerChatEvent) {
        if ("jndi:ldap" in event.message) {
            // This will kick the user if secure profile is on
            event.result = PlayerChatEvent.ChatResult.denied()
            event.player.disconnect(Component.text("Invalid message."))
            return
        }

        sendChat(event.message, event.player)
        if (event.message.lowercase().toSet().intersect(('a'..'z').toSet()).size > 3) {
            chatXPHandler.addXP(event.player.uniqueId)
        }
    }

    @Subscribe(priority = -99)
    fun onCommand(event: CommandExecuteEvent) {
        val player = event.commandSource as? Player ?: return

        // Extract base command without arguments
        val command = event.command.substringBefore(" ")

        if ("jndi:ldap" in event.command) {
            event.result = CommandExecuteEvent.CommandResult.denied()
            player.disconnect(Component.text("Invalid command."))
        }
        // Relay if publicly visible. Prepend / for special parsing.
        else if (command in RELAY_CMDS) sendChat("/${event.command}", player)
    }

    @Subscribe
    fun onQuery(event: ProxyQueryEvent) {
        // Only allow local queries
        if (!event.querierAddress.isSiteLocalAddress)
        // Make it empty
            event.response = QueryResponse.builder().build()
    }

    @Subscribe
    fun onPing(event: ProxyPingEvent) {
        val address = event.connection.remoteAddress.address
        // Always allow local queries or 1.13+ clients
        if (address.isSiteLocalAddress ||
            event.connection.protocolVersion > ProtocolVersion.MINECRAFT_1_13
        )
            return

        // Otherwise assume it's a bot
        this.logger.info("Blocked ping $address ${event.connection.protocolVersion}")
        event.result = ResultedEvent.GenericResult.denied()
    }

    @Subscribe(priority = 99)
    fun onConnect(event: ServerConnectedEvent) {
        val current = event.server.serverInfo.name
        lastServer[event.player.uniqueId] = current

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
            var greet =
                "Remember to read: /nrules | No theft.§x§a§e§r§o§w§m§n§e§t§h§e§r§i§s§f§a§i§r"
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
        val ip = player.remoteAddress?.address?.hostAddress ?: return
        val uuid = player.uniqueId

        tzBot.addPlayer(uuid, ip, isOnlinePredicate)
    }

    @Subscribe(priority = 99)
    fun onDisconnect(event: DisconnectEvent) {
        val fallbackName = lastServer.remove(event.player.uniqueId)

        val server =
            event.player.currentServer.orElse(null)?.server?.serverInfo?.name ?: fallbackName

        // Unsuccessful login. Don't print.
        if (server == null) return

        // Remove timezone
        tzBot.removePlayer(event.player.uniqueId)

        val name = event.player.username
        sendEvt(makeJoinQuit(username = name, serverName = server, type = "Quit"))
    }
}
