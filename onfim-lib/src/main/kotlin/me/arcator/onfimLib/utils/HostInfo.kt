package me.arcator.onfimLib.utils

import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Locale.getDefault

internal val rawHostname: String =
    try {
        InetAddress.getLocalHost().hostName
    } catch (e: UnknownHostException) {
        println("[Onfim] Hostname not found.")
        "styx"
    }

internal val hostname: String =
    when (rawHostname) {
        "pc00" -> "icarus"
        "pc01" -> "styx"
        else -> rawHostname
    }

internal val hostnameTitle = hostname.replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(
        getDefault(),
    ) else it.toString()
}

internal val s_id: Int =
    when (hostname) {
        "jylina" -> 1
        "thoth" -> 2
        "icarus" -> 3
        "styx" -> 4
        "suse" -> 5
        "juno" -> 6
        "apollo" -> 7
        "sputnik" -> 9
        "vulcan" -> 10
        else -> {
            println("Unexpected hostname. $hostname")
            8
        }
    }

internal const val SELF_PORT = 2403
val nodeNameS = "BG mcsa@$hostname"
val bind_ip = "10.0.0.$s_id"
