package me.arcator.onfimVelocity

import com.velocitypowered.api.command.SimpleCommand
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

class GlobalCommand(private val toggle: (Boolean, String) -> Unit) : SimpleCommand {
    override fun execute(invocation: SimpleCommand.Invocation) {
        val player = invocation.source()

        if (!player.hasPermission("onfim.globalrelay")) {
            player.sendMessage(
                Component.text(
                    "You do not have permission to toggle the global relay. Use /togglerelay instead.",
                    NamedTextColor.RED,
                )
            )
            return
        }

        val arg = invocation.arguments().firstOrNull()
        if (arg.equals("off", true)) {
            val text = "Disabled global relay."
            toggle(true, text)
            player.sendMessage(Component.text(text, NamedTextColor.RED))
            return
        }
        if (arg.equals("on", true)) {
            val text = "Enabled global relay."
            toggle(false, text)
            player.sendMessage(Component.text(text, NamedTextColor.GREEN))
            return
        }

        player.sendMessage(Component.text("Toggle `on` or `off`."))
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
