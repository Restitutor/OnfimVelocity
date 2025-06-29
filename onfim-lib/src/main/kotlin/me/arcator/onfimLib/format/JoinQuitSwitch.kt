package me.arcator.onfimLib.format

import net.kyori.adventure.text.format.NamedTextColor

class JoinQuit(
    val username: String,
    type: String,
    val game: Boolean = true,
    val platform: String = "In-Game",
    val server: EventLocation,
) : SerializedEvent(type = type), PlayerMoveInterface {

    override fun colour(): NamedTextColor =
        if (type == "Join") NamedTextColor.GREEN else NamedTextColor.RED

    override fun printString(): String {
        val vc = if (game) "" else " [VC]"
        val verb = if (type == "Join") " joined " else " left "
        return username + vc + verb + server.name
    }
}

fun cleanName(username: String) = if (username === "RestitutorOrb") "Restitutor" else username

fun makeJoinQuit(username: String, serverName: String, type: String) =
    JoinQuit(cleanName(username), server = EventLocation(name = serverName), type = type)

class Switch(
    val username: String,
    val fromServer: String,
    val game: Boolean = true,
    val platform: String = "In-Game",
    val server: EventLocation,
) : SerializedEvent(type = "Switch"), PlayerMoveInterface {

    override fun colour(): NamedTextColor = NamedTextColor.YELLOW

    override fun printString(): String {
        val vc = if (game) "" else " [VC]"
        return "${cleanName(username)}$vc moved from $fromServer to ${server.name}"
    }
}

fun makeSwitch(username: String, serverName: String, fromServer: String) =
    Switch(username, fromServer = fromServer, server = EventLocation(name = serverName))

@Suppress("unused")
fun SJoin(username: String, serverName: String) =
    makeJoinQuit(username = username, serverName = serverName, type = "SJoin")

@Suppress("unused")
fun SQuit(username: String, serverName: String) =
    makeJoinQuit(username = username, serverName = serverName, type = "SQuit")
