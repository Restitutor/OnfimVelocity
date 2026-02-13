package me.arcator.onfimVelocity

import com.m3z0id.tzbot4j.TZBot4J
import com.m3z0id.tzbot4j.tzLib.net.TZRequest
import com.m3z0id.tzbot4j.tzLib.net.c2s.IsLinkedData
import com.m3z0id.tzbot4j.tzLib.net.c2s.TimezonePostData
import com.mojang.brigadier.Command
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.ProxyServer
import java.net.URI
import java.time.ZoneId
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Predicate
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

class TimezoneCommand(
    private val server: ProxyServer,
    private val tzBot: TZBot4J,
    private val chatXPHandler: ChatXPHandler,
    private val isOnlinePredicate: Predicate<UUID> = Predicate<UUID> { uuid -> server.getPlayer(uuid).isPresent }
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
                BrigadierCommand.literalArgumentBuilder("unlink")
                    .executes { ctx: CommandContext<CommandSource> -> unlink(ctx.source) },

            )
            .then(
                BrigadierCommand.literalArgumentBuilder("refreshTimezone")
                    .executes { ctx: CommandContext<CommandSource> -> refreshTimezone(ctx.source) },
            )
            .build()

        return BrigadierCommand(timezoneNode)
    }

    private fun checkIfLinked(uuid: UUID): String? {
        val responsePromise = tzBot.queueRequest(TZRequest(IsLinkedData(uuid)))
        val response = responsePromise.get()

        return if (response.isSuccessful) response.asString else null
    }

    private fun checkIfLinkedLoop(runTimes: Int, uuid: UUID): String? {
        if (runTimes <= 0) return null
        var run = runTimes

        val response = checkIfLinked(uuid)
        if (response != null) return response
        --run

        Thread.sleep(TimeUnit.SECONDS.toMillis(5))
        return checkIfLinkedLoop(run, uuid)
    }

    private fun linkUserId(source: CommandSource): Int {
        if (source !is Player) {
            source.sendMessage(
                Component.text("Only executable by players!").color(NamedTextColor.RED),
            )
            return Command.SINGLE_SUCCESS
        }

        if (!tzBot.isTZBotUp) {
            source.sendMessage(
                Component.text("Timezone service is down. Please, try again later")
                    .color(NamedTextColor.RED),
            )
            return Command.SINGLE_SUCCESS
        }

        val zone: ZoneId? = tzBot.tzManager.getTimezone(source.uniqueId)
        if (zone == null) {
            source.sendMessage(
                Component.text(
                    "Error: Your timezone is not assigned!",
                    NamedTextColor.RED,
                ),
            )
            return Command.SINGLE_SUCCESS
        }

        val codeResponsePromise =
            tzBot.queueRequest(TZRequest(TimezonePostData(source.uniqueId, zone.id)))
        val response = codeResponsePromise.get()
        if (!response.isSuccessful || response.asString == null) {
            source.sendMessage(
                Component.text("Failed to create a code for you. Maybe you are already linked?")
                    .color(NamedTextColor.RED),
            )
            return Command.SINGLE_SUCCESS
        }

        val code: String = response.asString!!

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

        val runTimes = 180

        val accoutLinked = checkIfLinkedLoop(runTimes, source.uniqueId)
        if (accoutLinked != null) {
            source.sendMessage(
                Component.text("Linked successfully with $accoutLinked on Discord!")
                    .color(NamedTextColor.GREEN),
            )
            chatXPHandler.deleteFromNotLinked(source.uniqueId)
            chatXPHandler.addUUID(source.uniqueId, accoutLinked.toLong())
        } else {
            source.sendMessage(Component.text("Your code has timed out!").color(NamedTextColor.RED))
        }
        return Command.SINGLE_SUCCESS
    }

    private fun unlink(source: CommandSource): Int {
        if (source !is Player) {
            source.sendMessage(
                Component.text("Only executable by players!").color(NamedTextColor.RED),
            )
            return Command.SINGLE_SUCCESS
        }

        if(checkIfLinked(source.uniqueId) == null) {
            source.sendMessage(
                Component.text("You aren't linked to any account on Discord!").color(NamedTextColor.RED),
            )
            return Command.SINGLE_SUCCESS
        }

        val discordComponent = Component.text("Discord")
            .decorate(TextDecoration.UNDERLINED)
            .color(NamedTextColor.BLUE)
            .hoverEvent(HoverEvent.showText(Component.text("Click to join!")))
            .clickEvent(ClickEvent.openUrl(URI.create("https://discord.gg/GwArgw2").toURL()))

        val msg = Component.text("Please, confirm the unlinking on our ")
            .append(discordComponent)
            .append(Component.text(" and run /unlink from TimezoneBot#8433"))

        source.sendMessage(msg)
        return Command.SINGLE_SUCCESS
    }

    private fun getTimezone(source: CommandSource): Int {
        if (source !is Player) {
            source.sendMessage(
                Component.text("Only executable by players!").color(NamedTextColor.RED),
            )
            return Command.SINGLE_SUCCESS
        }

        val timezone = tzBot.tzManager.getTimezone(source.uniqueId)
        if (timezone == null) {
            source.sendMessage(
                Component.text("Your timezone has not been assigned! It should assign soon.").color(
                    NamedTextColor.RED,
                ),
            )
        } else {
            source.sendMessage(
                Component.text("Your current timezone is: ${timezone.id}")
                    .color(NamedTextColor.GREEN),
            )
        }

        return Command.SINGLE_SUCCESS
    }

    private fun refreshTimezone(source: CommandSource): Int {
        if (source is Player) {
            val uuid: UUID = source.uniqueId
            val ip = source.remoteAddress?.address?.hostAddress ?: return Command.SINGLE_SUCCESS

            if (!tzBot.isTZBotUp) {
                source.sendMessage(
                    Component.text("Timezone service is down. Please, try again later")
                        .color(NamedTextColor.RED),
                )
                return Command.SINGLE_SUCCESS
            }

            tzBot.removePlayer(uuid)
            tzBot.addPlayer(uuid, ip, isOnlinePredicate)
            source.sendMessage(
                Component.text("Timezone refreshed successfully!").color(NamedTextColor.GREEN),
            )
        } else {
            source.sendMessage(
                Component.text("Only executable by players!").color(NamedTextColor.RED),
            )
        }
        return Command.SINGLE_SUCCESS
    }
}
