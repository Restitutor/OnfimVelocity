package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import me.arcator.onfimLib.utils.hostname
import me.arcator.onfimLib.utils.nodeNameS
import me.arcator.onfimLib.utils.s_id

class NodeInfo(val type: String = "BG", val host: String = hostname, val name: String = nodeNameS)

class EventLocation(val arcator: Boolean = true, val name: String, val id: String? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
open class SerializedEvent(
    val type: String,
    val id: Int = randomEvtId(),
    val node: NodeInfo = NodeInfo(),
) {
    companion object {
        fun randomEvtId(): Int {
            // Random 7 digit code
            return (0..999999).random() * 10 + s_id - 1
        }
    }

    @JsonIgnore
    fun shouldRelay() = nodeNameS != node.name
}
