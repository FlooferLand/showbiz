package com.flooferland.showbiz.types.commands

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.Suggestions
import com.mojang.brigadier.suggestion.SuggestionsBuilder
import java.util.concurrent.CompletableFuture

typealias SuggestBlock = (ctx: CommandContext, builder: SuggestionsBuilder) -> CompletableFuture<Suggestions>

data class CommandArgument<T>(val name: String, val type: ArgumentType<T>, val clazz: Class<T>, var isOptional: Boolean = false) {
    var suggest: SuggestBlock? = null
    fun suggests(block: SuggestBlock) = also { suggest = block }

    data class Optional<T>(val arg: CommandArgument<T>)
    fun optional() = Optional(this)

    class Registry(val command: Command, val list: MutableList<CommandArgument<*>> = mutableListOf()) {
        private fun <T> add(block: () -> CommandArgument<T>): CommandArgument<T> {
            val argument = block()
            if (list.any { it.name == argument.name })
                error("Argument '${argument.name}' already exists on '${command.name}'")
            list += argument
            return argument
        }

        fun string(name: String) = add() {
            CommandArgument<String>(name, StringArgumentType.string(), String::class.java)
        }
        fun word(name: String) = add() {
            CommandArgument<String>(name, StringArgumentType.word(), String::class.java)
        }

        fun int(name: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE) = add() {
            CommandArgument<Int>(name, IntegerArgumentType.integer(min, max), Int::class.java)
        }
    }
}