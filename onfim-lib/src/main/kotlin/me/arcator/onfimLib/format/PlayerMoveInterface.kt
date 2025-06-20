package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface PlayerMoveInterface {
    @JsonIgnore
    fun colour(): NamedTextColor

    @JsonIgnore
    fun printString(): String

    @JsonIgnore
    fun getComponent() = Component.text(printString(), colour())
}
