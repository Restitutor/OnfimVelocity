package me.arcator.onfimVelocity.timezone.command

import com.mojang.brigadier.LiteralMessage
import com.mojang.brigadier.StringReader
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.exceptions.CommandSyntaxException
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.*
import java.util.concurrent.CompletableFuture
import me.arcator.onfimVelocity.timezone.Timezone

class TimezoneArgumentType : ArgumentType<String> {
    @Throws(CommandSyntaxException::class)
    override fun parse(reader: StringReader): String {
        val start = reader.cursor
        while (reader.canRead()) reader.skip()

        val timezone = reader.string.substring(start, reader.cursor)
        if (Timezone.TIMEZONES_STRING.contains(timezone)) return timezone
        throw SimpleCommandExceptionType(LiteralMessage("You must provide a valid timezone!"))
            .createWithContext(reader)
    }

    override fun <S> listSuggestions(context: CommandContext<S>, builder: SuggestionsBuilder): CompletableFuture<Suggestions> {
        val input = builder.remaining.lowercase(Locale.getDefault())
        val alreadySuggested: MutableList<String> = mutableListOf()
        Timezone.TIMEZONES.stream()
            .filter { e: Map<String, String> -> e.values.first().lowercase(Locale.getDefault()).startsWith(input) }
            .map { e: Map<String, String> -> e.keys.first() + "/" + e.values.first() }
            .forEach { text: String ->
                builder.suggest(text)
                alreadySuggested.add(text)
            }

        Timezone.TIMEZONES.stream()
            .filter{ e: Map<String, String> -> e.keys.first().lowercase(Locale.getDefault()).startsWith(input) }
            .map { e: Map<String, String> -> e.keys.first() + "/" + e.values.first() }
            .filter { text: String -> !alreadySuggested.contains(text) }
            .forEach { text: String -> builder.suggest(text) }

        return builder.buildFuture()
    }

    override fun getExamples(): Collection<String> {
        return Timezone.TIMEZONES_STRING
    }

    companion object {
        fun timezone(): TimezoneArgumentType {
            return TimezoneArgumentType()
        }
    }
}
