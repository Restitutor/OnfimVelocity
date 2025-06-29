package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.*

val RES_ID: UUID = UUID.fromString(System.getenv("RES_MC_ID"))

@JsonIgnoreProperties(ignoreUnknown = true)
class ChatRoom(val id: String = "#arcator", val name: String? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
class ChatUser(
    val name: String,
    val bot: Boolean = false,
    uuid: UUID? = null, // null for incoming events
    val id: String? = uuid.toString(),
    val colour: String? = null,
    val auth: String? =
        if (uuid == RES_ID) {
            "RestitutorOrbis"
        } else null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
class DiscordContext(
    val replyColour: String? = null,
    val replyUser: String? = null,
    val replyText: String? = null,
)
