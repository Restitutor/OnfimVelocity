package me.arcator.onfimVelocity

import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.io.path.writeText

// Saves and writes sets of UUID to newline delimited txt file
class PersistSet(private val file: Path) {
    var players = mutableSetOf<UUID>()

    init {
        // Create data directory if it doesn't exist
        if (Files.notExists(file.parent)) {
            Files.createDirectories(file.parent)
        }

        if (file.exists()) {
            players = file.readLines().map { UUID.fromString(it) }.toHashSet()
        }
    }

    fun save() {
        file.writeText(players.joinToString("\n"))
    }
}
