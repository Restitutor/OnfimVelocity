package me.arcator.onfimVelocity

import com.velocitypowered.api.command.SimpleCommand
import com.velocitypowered.api.proxy.Player
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class ToggleCommand(private val noPlayers: UUIDSet, private val name: String) : SimpleCommand {
    override fun execute(invocation: SimpleCommand.Invocation) {
        val player = invocation.source()
        if (player !is Player) {
            player.sendMessage(
                Component.text("This command is only for players.", NamedTextColor.RED)
            )
            return
        }

        val arg = invocation.arguments().firstOrNull()
        val component =
            if (arg == "off" || (arg == null && !noPlayers.contains(player.uniqueId))) {
                noPlayers.add(player.uniqueId)
                Component.text("Disabled Discord $name relay.", NamedTextColor.RED)
            } else {
                noPlayers.remove(player.uniqueId)
                Component.text("Enabled Discord $name relay.", NamedTextColor.GREEN)
            }
        player.sendMessage(component)
    }

    override fun suggest(invocation: SimpleCommand.Invocation?): List<String> {
        if (invocation == null) return emptyList()

        val args: Array<out String> = invocation.arguments()

        val result = mutableListOf<String>()
        if (args.size == 1) {
            if ("on".startsWith(args[0])) result.add("on")
            if ("off".startsWith(args[0])) result.add("off")
        }

        return result
    }
}
