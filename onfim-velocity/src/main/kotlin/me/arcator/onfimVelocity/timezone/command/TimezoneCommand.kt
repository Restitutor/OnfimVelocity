package me.arcator.onfimVelocity.timezone.command

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.command.CommandSource
import com.velocitypowered.api.proxy.Player
import java.net.URI
import java.util.*
import me.arcator.onfimVelocity.timezone.TZRequests
import me.arcator.onfimVelocity.timezone.Timezone
import me.arcator.onfimVelocity.timezone.Timezone.Companion.TIMEZONES_STRING
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration

class TimezoneCommand(private val tz: Timezone) {
    fun createTimezoneCommand(): BrigadierCommand {
        val timezoneNode = BrigadierCommand.literalArgumentBuilder("timezone")
            .requires { source: CommandSource -> source is Player }
            .executes { ctx: CommandContext<CommandSource> -> getTimezone(ctx.source) }
            .then(BrigadierCommand.literalArgumentBuilder("link")
                .executes { ctx: CommandContext<CommandSource> -> linkUserId(ctx.source) })
            .then(BrigadierCommand.literalArgumentBuilder("setTimezone")
                .then(BrigadierCommand.requiredArgumentBuilder("timezone", StringArgumentType.greedyString())
                    // Velocity inablilty fix
                    .suggests { _, builder ->
                        val input = builder.remaining.lowercase(Locale.getDefault())
                        val alreadySuggested: MutableList<String> = mutableListOf()
                        Timezone.TIMEZONES.iterator()
                            .asSequence()
                            .filter { it["city"]?.lowercase(Locale.getDefault())!!.startsWith(input) }
                            .map { "${it["area"]}/${it["city"]}" }
                            .onEach { text ->
                                builder.suggest(text)
                                alreadySuggested.add(text)
                            }
                        Timezone.TIMEZONES.iterator()
                            .asSequence()
                            .filter { it["area"]?.lowercase(Locale.getDefault())!!.startsWith(input) }
                            .map { "${it["area"]}/${it["city"]}" }
                            .filterNot { alreadySuggested.contains(it) }
                            .forEach { builder.suggest(it) }
                        builder.buildFuture()
                    }
                    .executes { ctx: CommandContext<CommandSource> -> setUserTimezone(ctx.source, ctx.getArgument("timezone", String::class.java)) }))
            .then(BrigadierCommand.literalArgumentBuilder("clearTimezone")
                .executes {ctx: CommandContext<CommandSource> -> removeOverride(ctx.source)})
            .build()

        return BrigadierCommand(timezoneNode)
    }

    private fun linkUserId(source: CommandSource): Int {
        if(source is Player) {
            val code: String? = TZRequests.sendUserIdUUIDLinkPost(source.uniqueId)
            if(code == null){
                source.sendMessage(Component.text("Failed to create a code for you. Maybe you are already linked?").color(NamedTextColor.RED))
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
        }
        return Command.SINGLE_SUCCESS
    }

    private fun getTimezone(source: CommandSource): Int {
        if(source is Player) {
            val timezone: String = tz.getPlayerTimezone(source.uniqueId).id
            source.sendMessage(Component.text("Your current timezone is: $timezone").color(NamedTextColor.GREEN))
        } else {
            source.sendMessage(Component.text("Only executable by players!").color(NamedTextColor.RED))
        }
        return Command.SINGLE_SUCCESS
    }

    private fun setUserTimezone(source: CommandSource, timezone: String): Int {
        if(timezone !in TIMEZONES_STRING) {
            source.sendMessage(Component.text("Wrong timezone!").color(NamedTextColor.RED))
            return Command.SINGLE_SUCCESS
        }
        if(source is Player) {
            val uuid: UUID = source.uniqueId
            TZRequests.sendTZOverridePost(uuid, timezone)
            tz.addTimezone(source.uniqueId, timezone)
            source.sendMessage(Component.text("Your timezone has been changed!").color(NamedTextColor.GREEN))
        } else {
            source.sendMessage(Component.text("Only executable by players!").color(NamedTextColor.RED))
        }
        return Command.SINGLE_SUCCESS
    }

    private fun removeOverride(source: CommandSource): Int {
        if(source is Player) {
            val uuid: UUID = source.uniqueId
            if(!TZRequests.sendTZOverrideRemove(uuid)) {
                source.sendMessage(Component.text("There's nothing to remove!").color(NamedTextColor.RED))
                return Command.SINGLE_SUCCESS
            }
            tz.removeOverride(uuid)
            tz.removeUUID(uuid)
            tz.addPlayer(uuid, source.remoteAddress.address.hostAddress)
            source.sendMessage(Component.text("Timezone override was cleared!").color(NamedTextColor.GREEN))
        } else {
            source.sendMessage(Component.text("Only executable by players!").color(NamedTextColor.RED))
        }
        return Command.SINGLE_SUCCESS
    }
}
