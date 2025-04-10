package me.arcator.onfimVelocity

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import java.util.*
import kotlin.jvm.optionals.getOrNull
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class ToggleCommand(private val getPlayer: (String) -> Optional<Player>, private val noPlayers: UUIDSet, private val name: String) : SimpleCommand {
    override fun execute(invocation: SimpleCommand.Invocation) {
        val source = invocation.source()

        // Check for player names
        var p = invocation.arguments()
            .map(getPlayer).firstNotNullOfOrNull { it.getOrNull() }

        if (p == null) {
            if (source is Player) {
                p = source
            } else {
                source.sendMessage(
                    Component.text("No player provided!", NamedTextColor.RED)
                )
                return
            }
        }

        val uuid = p.uniqueId
        val label = "${p.username} Discord $name"
        val component =
            if ("off" in invocation.arguments() || !noPlayers.contains(uuid)) {
                noPlayers.add(uuid)
                Component.text("Disabled $label relay.", NamedTextColor.RED)
            } else {
                noPlayers.remove(uuid)
                Component.text("Enabled $label relay.", NamedTextColor.GREEN)
            }
        source.sendMessage(component)
    }

    override fun suggest(invocation: SimpleCommand.Invocation?): List<String> {
        if (invocation == null) return emptyList()

        val args = invocation.arguments()
        val result = mutableListOf<String>()
        if (args.size == 1) {
            if ("on".startsWith(args[0])) result.add("on")
            if ("off".startsWith(args[0])) result.add("off")
        }

        return result
    }
}
