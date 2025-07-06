package me.arcator.onfimVelocity.timezone

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class Timezone {
    private val playerTimezones: MutableMap<UUID, ZoneId> = mutableMapOf()
    private val timezoneOverrides: MutableMap<UUID, ZoneId> = mutableMapOf()

    private fun formatRelativeTime(unixTimestamp: Long): String {
        if (unixTimestamp == System.currentTimeMillis() / 1000) return "now"

        val timestamp = Instant.ofEpochSecond(unixTimestamp)
        val now = Instant.now()
        var duration = Duration.between(timestamp, now)

        val isFuture = duration.isNegative
        duration = duration.abs()

        val years = (duration.toDays() / 365.25).toInt()
        val days = (duration.toDays() % 365.25).toInt()
        val hours = duration.toHours() % 24
        val minutes = duration.toMinutes() % 60
        val seconds = duration.seconds % 60

        val result = StringBuilder()
        if (years > 0) result.append(years).append(" year").append(if (years > 1) "s" else "")
            .append(", ")
        if (days > 0) result.append(days).append(" day").append(if (days > 1) "s" else "")
            .append(", ")
        if (hours > 0) result.append(hours).append(" hour").append(if (hours > 1) "s" else "")
            .append(", ")
        if (minutes > 0) result.append(minutes).append(" minute")
            .append(if (minutes > 1) "s" else "").append(", ")
        if (seconds > 0) result.append(seconds).append(" second")
            .append(if (seconds > 1) "s" else "")

        return result.toString()
            .replace(", $".toRegex(), "") + (if (isFuture) " from now" else " ago")
    }

    fun getTime(playerUUID: UUID, timestamp: Long, mode: Char): String {
        val zoneId = playerTimezones[playerUUID] ?: ZoneId.systemDefault()
        val requestedInstant = Instant.ofEpochSecond(timestamp)

        when (mode) {
            'f' -> return DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm")
                .format(requestedInstant.atZone(zoneId))

            'F' -> return DateTimeFormatter.ofPattern("EEEE, MMMM dd, yyyy 'at' HH:mm")
                .format(requestedInstant.atZone(zoneId))

            'd' -> return DateTimeFormatter.ofPattern("dd/MM/yyyy")
                .format(requestedInstant.atZone(zoneId))

            'D' -> return DateTimeFormatter.ofPattern("MMMM dd, yyyy")
                .format(requestedInstant.atZone(zoneId))

            't' -> return DateTimeFormatter.ofPattern("HH:mm")
                .format(requestedInstant.atZone(zoneId))

            'T' -> DateTimeFormatter.ofPattern("HH:mm:ss")
                .format(requestedInstant.atZone(zoneId))

            'R' -> return formatRelativeTime(timestamp)
        }
        return ""
    }

    fun addPlayer(uuid: UUID, ip: String) {
        if(uuid in timezoneOverrides) playerTimezones[uuid] = timezoneOverrides[uuid]!!
        if (uuid !in playerTimezones) {
            val timezone =
                TZRequests.sendTZFromUUID(uuid) ?: TZRequests.sendIPTZRequest(ip) ?: return
            playerTimezones[uuid] = ZoneId.of(timezone)
        }
    }

    fun getPlayerTimezone(uuid: UUID): ZoneId {
        return playerTimezones[uuid]!!
    }

    fun addTimezone(uuid: UUID, timezone: String) {
        playerTimezones[uuid] = ZoneId.of(timezone)
    }

    fun removeUUID(uuid: UUID) {
        playerTimezones.remove(uuid)
    }

    fun setOverrides(map: Map<UUID, String>) {
        map.mapValues { (key, value) ->
            timezoneOverrides[key] = ZoneId.of(value)
        }
    }

    fun removeOverride(uuid: UUID) {
        timezoneOverrides.remove(uuid)
    }
}
