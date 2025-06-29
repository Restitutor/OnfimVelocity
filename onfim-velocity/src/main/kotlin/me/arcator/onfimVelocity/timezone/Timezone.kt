package me.arcator.onfimVelocity.timezone

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

class Timezone {
    private val playerTimezones: MutableMap<UUID, ZoneId> = mutableMapOf()

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
        var mark = ""
        var zoneId = playerTimezones[playerUUID]

        if (zoneId == null) {
            mark = "."
            zoneId = ZoneId.systemDefault()
        }

        val calendar = Calendar.getInstance()
        calendar.timeZone = TimeZone.getTimeZone(zoneId)
        calendar.time = Date.from(Instant.ofEpochSecond(timestamp))

        when (mode) {
            '?' -> {
                val date = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
                    .format(calendar.toInstant().atZone(zoneId))
                val time =
                    DateTimeFormatter.ofPattern("HH:mm").format(calendar.toInstant().atZone(zoneId))
                return "$mark$date at $time"
            }

            'f' -> {
                val date = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
                    .format(calendar.toInstant().atZone(zoneId))
                val time =
                    DateTimeFormatter.ofPattern("HH:mm").format(calendar.toInstant().atZone(zoneId))
                return "$mark$date at $time"
            }

            'F' -> {
                val dayInt = calendar[Calendar.DAY_OF_WEEK]
                val day =
                    if (dayInt == Calendar.SUNDAY) "Sunday" else if (dayInt == Calendar.MONDAY) "Monday" else if (dayInt == Calendar.TUESDAY) "Tuesday" else if (dayInt == Calendar.WEDNESDAY) "Wednesday" else if (dayInt == Calendar.THURSDAY) "Thursday" else if (dayInt == Calendar.FRIDAY) "Friday" else "Saturday"
                val date = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
                    .format(calendar.toInstant().atZone(zoneId))
                val time =
                    DateTimeFormatter.ofPattern("HH:mm").format(calendar.toInstant().atZone(zoneId))

                return "$mark$day, $date at $time"
            }

            'd' -> {
                val date = DateTimeFormatter.ofPattern("dd/MM/yyyy")
                    .format(calendar.toInstant().atZone(zoneId))
                return "$mark$date"
            }

            'D' -> {
                val date = DateTimeFormatter.ofPattern("MMMM dd, yyyy")
                    .format(calendar.toInstant().atZone(zoneId))
                return "$mark$date"
            }

            't' -> {
                val time =
                    DateTimeFormatter.ofPattern("HH:mm").format(calendar.toInstant().atZone(zoneId))
                return "$mark$time"
            }

            'T' -> {
                val time = DateTimeFormatter.ofPattern("HH:mm:ss")
                    .format(calendar.toInstant().atZone(zoneId))
                return "$mark$time"
            }

            'R' -> {
                return formatRelativeTime(timestamp)
            }

            else -> {
                return ""
            }
        }
    }

    fun extractTimestamp(tag: String): Long {
        val regex = Regex("<t:(\\d+):([fFDdtTR])>")
        val matchResult = regex.find(tag)

        return matchResult!!.groups[1]!!.value.toLong()
    }

    fun addPlayer(uuid: UUID, username: String, ip: String) {
        if (uuid !in playerTimezones) {
            val timezone =
                TZRequests.sendAliasTZRequest(username) ?: TZRequests.sendIPTZRequest(ip) ?: return
            playerTimezones[uuid] = ZoneId.of(timezone)
        }
    }

    fun removeUUID(uuid: UUID) {
        if (playerTimezones.size > 100) playerTimezones.remove(uuid)
    }
}
