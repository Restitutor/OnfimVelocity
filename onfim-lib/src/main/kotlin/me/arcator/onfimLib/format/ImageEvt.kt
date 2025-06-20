package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.nio.charset.StandardCharsets.UTF_8
import java.util.zip.GZIPInputStream
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor

@JsonIgnoreProperties(ignoreUnknown = true)
class ImageEvt(private val content: ByteArray, val name: String, val width: Int, val height: Int) :
    SerializedEvent(type = "Image") {

    private fun getRGBArray() =
        GZIPInputStream(content.inputStream()).bufferedReader(UTF_8).readText()

    @Suppress("unused")
    fun getLines() = iterator {
        var comp = Component.text()
        val colours = getRGBArray()

        for (i in 0..<getSize()) {
            val startIndex = i * 6
            val colour = colours.substring(startIndex, startIndex + 6)
            comp.append(Component.text("▏", TextColor.fromCSSHexString("#$colour")))

            if (i % width == width - 1) {
                yield(comp.build())
                comp = Component.text()
            }
        }
    }

    private fun getSize() = width * height
}
