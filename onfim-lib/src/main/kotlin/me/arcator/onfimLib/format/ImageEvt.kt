package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.util.zip.GZIPInputStream
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

@JsonIgnoreProperties(ignoreUnknown = true)
class ImageEvt(private val content: ByteArray, val name: String, val width: Int, val height: Int) :
    SerializedEvent(type = "Image") {

    @Suppress("unused")
    fun getLines() = iterator {
        GZIPInputStream(content.inputStream()).use { gzipStream ->
            var pixelIndex = 0
            var comp = Component.text()

            while (true) {
                val pixel = gzipStream.readNBytes(3)
                if (pixel.size < 3) break // EOF or incomplete pixel

                val r = pixel[0].toInt() and 0xFF
                val g = pixel[1].toInt() and 0xFF
                val b = pixel[2].toInt() and 0xFF
                val hexColor = String.format("#%02X%02X%02X", r, g, b)

                comp.append(Component.text("▏", TextColor.fromCSSHexString(hexColor)))

                pixelIndex++
                if (pixelIndex % width == 0) {
                    yield(comp.build())
                    comp = Component.text()
                }
            }

            // Yield any remaining pixels on the last line
            if (pixelIndex % width != 0) {
                yield(comp.build())
            }
        }
    }
}
