package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import me.arcator.onfimLib.out.Port

@JsonIgnoreProperties(ignoreUnknown = true)
class Heartbeat(val sctp: Port? = null) :
    SerializedEvent(type = "Heartbeat")
