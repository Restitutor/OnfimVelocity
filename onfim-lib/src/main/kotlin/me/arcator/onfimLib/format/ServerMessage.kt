package me.arcator.onfimLib.format


import me.arcator.onfimLib.utils.hostnameTitle
import net.kyori.adventure.text.Component

class ServerMessage(
    val text: String,
    val server: String = "Velocity$hostnameTitle"
) :
    SerializedEvent(type = "Server Message") {
    fun getComponent() = Component.text("$server - $text")
}
