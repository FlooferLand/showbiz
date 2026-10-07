package com.flooferland.showbiz.types.commands

import net.minecraft.commands.*
import com.mojang.brigadier.context.CommandContext

class CommandContext(val inner: CommandContext<CommandSourceStack>, val source: CommandSource = CommandSource(inner.source)) {
    fun <T> getArgument(arg: CommandArgument<T>): T =
        inner.getArgument(arg.name, arg.clazz)
    fun <T> getArgument(arg: CommandArgument.Optional<T>): T? =
        if (hasArgument(arg.arg)) getArgument(arg.arg) else null
    fun <T> hasArgument(arg: CommandArgument<T>): Boolean =
        try { inner.getArgument(arg.name, arg.clazz); true } catch (e: Exception) { false }
}