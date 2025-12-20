package me.arcator.onfimVelocity.timezone.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import java.net.URI
import java.util.*
import java.util.concurrent.TimeUnit
import me.arcator.onfimVelocity.OnfimVelocity
import me.arcator.onfimVelocity.timezone.TZRequests
import me.arcator.onfimVelocity.timezone.Timezone
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

class TimezoneCommand(
    private val plugin: OnfimVelocity,
    private val server: ProxyServer,
    private val tz: Timezone
) {
    fun createTimezoneCommand(): BrigadierCommand {
        val timezoneNode = BrigadierCommand.literalArgumentBuilder("timezone")
            .requires { source: CommandSource -> source is Player }
            .executes { ctx: CommandContext<CommandSource> -> getTimezone(ctx.source) }
            .then(
                BrigadierCommand.literalArgumentBuilder("link")
                    .executes { ctx: CommandContext<CommandSource> -> linkUserId(ctx.source) },
            )
            .then(
                BrigadierCommand.literalArgumentBuilder("refreshTimezone")
                    .executes { ctx: CommandContext<CommandSource> -> refreshTimezone(ctx.source) },
            )
            .build()

        return BrigadierCommand(timezoneNode)
    }

    private fun linkUserId(source: CommandSource): Int {
        if (source is Player) {
            val code: String? = TZRequests.sendUserIdUUIDLinkPost(
                source.uniqueId,
                tz.getPlayerTimezone(source.uniqueId),
            )
            if (code == null) {
                source.sendMessage(
                    Component.text("Failed to create a code for you. Maybe you are already linked?")
                        .color(NamedTextColor.RED),
                )
                return Command.SINGLE_SUCCESS
            }
            val msg = Component.text("Your authentication code is ")
            val codeComponent = Component.text(code)
                .decorate(TextDecoration.UNDERLINED)
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy!")))
                .clickEvent(ClickEvent.copyToClipboard(code))

            val discordComponent = Component.text("Discord")
                .decorate(TextDecoration.UNDERLINED)
                .color(NamedTextColor.BLUE)
                .hoverEvent(HoverEvent.showText(Component.text("Click to join!")))
                .clickEvent(ClickEvent.openUrl(URI.create("https://discord.gg/GwArgw2").toURL()))

            val message = msg.append(codeComponent)
                .append(Component.text(". "))
                .append(Component.text("Find Timezone Bot #8433 on our "))
                .append(discordComponent)
                .append(Component.text(" and run /link "))
                .append(codeComponent)

            source.sendMessage(message)

            var runTimes = 180
            server.scheduler.buildTask(plugin) { task ->
                if (runTimes == 0) {
                    task.cancel()
                    source.sendMessage(
                        Component.text("Your code has expired.").color(NamedTextColor.RED),
                    )
                } else if (TZRequests.sendIsLinkedRequest(source.uniqueId)) {
                    task.cancel()
                    val discordId = TZRequests.sendUserIDFromUUID(source.uniqueId)!!
                    source.sendMessage(
                        Component.text(
                            "Your accounts have been linked successfully! (Discord account ID: %s)".format(
                                discordId,
                            ),
                        ).color(NamedTextColor.GREEN),
                    )
                }
                --runTimes
            }.repeat(5L, TimeUnit.SECONDS)
                .schedule()
        }
        return Command.SINGLE_SUCCESS
    }

    private fun getTimezone(source: CommandSource): Int {
        if (source is Player) {
            val timezone: String = tz.getPlayerTimezone(source.uniqueId).id
            source.sendMessage(
                Component.text("Your current timezone is: $timezone").color(NamedTextColor.GREEN),
            )
        } else {
            source.sendMessage(
                Component.text("Only executable by players!").color(NamedTextColor.RED),
            )
        }
        return Command.SINGLE_SUCCESS
    }

    private fun refreshTimezone(source: CommandSource): Int {
        if (source is Player) {
            val uuid: UUID = source.uniqueId
            val ip = source.remoteAddress?.address?.hostAddress ?: return Command.SINGLE_SUCCESS

            if (!tz.addPlayer(uuid, ip)) {
                source.sendMessage(
                    Component.text("We failed to retrieve your timezone.").color(
                        NamedTextColor.RED,
                    ),
                )
            }
            source.sendMessage(
                Component.text("Timezone refreshed successfully!").color(
                    NamedTextColor.GREEN,
                ),
            )
        } else {
            source.sendMessage(
                Component.text("Only executable by players!").color(NamedTextColor.RED),
            )
        }
        return Command.SINGLE_SUCCESS
    }
}
