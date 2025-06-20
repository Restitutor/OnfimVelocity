package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import de.themoep.minedown.adventure.MineDown
import net.fellbaum.jemoji.EmojiManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

val RELAY_CMDS = hashSetOf("me", "eme", "broadcast", "bc", "say", "alert")

// #arcator, #arcator test, #ssmp, #polls
val DISCORD_CHANNELS = hashSetOf(
    System.getenv("discord_main_channel"),
    "1364820197801988099",
    "197512800950681600",
    "749467873491157062",
)

@JsonIgnoreProperties(ignoreUnknown = true)
class Chat(
    val plaintext: String,
    val user: ChatUser,
    val server: EventLocation,
    val rawtext: String = plaintext,
    val platform: String = "In-Game",
    val context: DiscordContext? = null,
    val mentioned: Boolean = plaintext.lowercase().split(" ").any { it.endsWith("fim") },
    val language: String? = null,
    val dm: Boolean = false,
    val perms: Int = 0,
    val room: ChatRoom = ChatRoom(),
) : SerializedEvent(type = "Chat") {
    @Suppress("unused")
    @JsonIgnore
    fun getChatMessage(): Component {
        val hover =
            if (context?.replyUser == null) {
                Component.text(getHover())
            } else {
                val comp =
                    Component.text(context.replyUser)
                        .append(Component.text(":\n", NamedTextColor.WHITE))

                var suffix = MineDown.parse(context.replyText)
                if (context.replyColour != null) {
                    suffix = suffix.color(TextColor.fromCSSHexString(context.replyColour))
                }
                comp.append(suffix)
            }

        var prefix =
            Component.text(user.name, getColour())
                .clickEvent(ClickEvent.openUrl("https://discord.gg/GwArgw2"))
                .hoverEvent(HoverEvent.showText(hover))

        if (context?.replyUser != null) {
            prefix = prefix.append(Component.text(" ⏎").decoration(TextDecoration.BOLD, true))
        }

        return prefix.append(Component.text(": ", NamedTextColor.WHITE))
            .append(MineDown.parse("&f$plaintext"))
    }

    private fun inGame() =
        ((platform == "Discord" && room.id in DISCORD_CHANNELS) ||
            (platform == "Onfim" && room.id == "#arcator") ||
            (platform == "Matrix" && room.id == System.getenv("matrix_main_channel")) ||
            platform == "In-Game")

    fun shouldShow() = inGame()

    @Suppress("unused")
    @JsonIgnore
    private fun getColour(): TextColor {
        val userColour = user.colour
        if ((userColour is String) && userColour.startsWith("#")) {
            return TextColor.fromCSSHexString(userColour)!!
        }

        return when (platform) {
            "In-Game" -> NamedTextColor.GOLD
            "Onfim" -> NamedTextColor.YELLOW
            "Discord" ->
                if (user.bot) NamedTextColor.BLUE else TextColor.fromCSSHexString("#5865F2")!!

            "Matrix" -> NamedTextColor.LIGHT_PURPLE
            else -> {
                println("[Onfim Listen] Did not expect platform: $platform")
                NamedTextColor.DARK_RED
            }
        }
    }

    companion object {
        @Suppress("unused")
        fun fromMessage(rawMsg: String): String {
            if (rawMsg.startsWith("/")) {
                var matchedCmd = false

                for (cmd in RELAY_CMDS) {
                    val fullCmd = "/$cmd "
                    if (rawMsg.startsWith(fullCmd)) {
                        matchedCmd = true
                        break
                    }
                }

                // Ignore all other commands
                if (!matchedCmd) return ""
            }
            val msg =
                if (rawMsg.startsWith("/me ")) {
                    val suffix: String =
                        rawMsg.split("/me ", ignoreCase = false, limit = 2).last()
                    "* $suffix"
                } else if (rawMsg.startsWith("/eme ")) {
                    val suffix: String =
                        rawMsg.split("/eme ", ignoreCase = false, limit = 2).last()
                    "* $suffix"
                } else {
                    rawMsg
                }

            // Disable all emojification on this line if there's mg (flag would override it)
            if (msg.contains(":mg:")) return msg

            return EmojiManager.replaceAliases(msg
            ) { alias, emojis ->
                emojis.stream().filter { emoji -> emoji.discordAliases.contains(alias) }
                    .findFirst().orElseThrow { IllegalStateException() }.emoji
            }
        }
    }

    @JsonIgnore
    fun getHover() = if (platform == "In-Game") server.name else "$platform ${room.name ?: ""}"
}
