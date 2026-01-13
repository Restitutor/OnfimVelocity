package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.zip.GZIPInputStream
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

@JsonIgnoreProperties(ignoreUnknown = true)
class ImageEvt(
    private val content: ByteArray,
    val name: String,
    val width: Int,
    val height: Int,
) : SerializedEvent(type = "Image") {
    @Suppress("unused")
    fun getLines() =
        iterator {
            GZIPInputStream(content.inputStream()).use { gzipStream ->
                var currentLineWidth = 0
                var comp = Component.text()

                // Reusable buffer to avoid allocation
                val packet = ByteArray(4)

                while (true) {
                    // Read exactly 4 bytes (R, G, B, Count)
                    if (gzipStream.readNBytes(packet, 0, 4) < 4) break

                    val r = packet[0].toInt() and 0xFF
                    val g = packet[1].toInt() and 0xFF
                    val b = packet[2].toInt() and 0xFF
                    val count = packet[3].toInt() and 0xFF

                    // Optimization: Use direct Int color instead of Hex String parsing
                    comp.append(Component.text("▏".repeat(count), TextColor.color(r, g, b)))

                    currentLineWidth += count

                    // Since Python guarantees runs don't overlap lines,
                    // we only check if we hit the exact edge.
                    if (currentLineWidth == width) {
                        yield(comp.build())
                        comp = Component.text()
                        currentLineWidth = 0
                    }
                }

                // Safety flush for non-standard sizes
                if (currentLineWidth > 0) {
                    yield(comp.build())
                }
            }
        }
}
