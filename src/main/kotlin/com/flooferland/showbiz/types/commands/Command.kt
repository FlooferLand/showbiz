package com.flooferland.showbiz.types.commands

import net.minecraft.*
import net.minecraft.commands.*
import net.minecraft.network.chat.*
import com.flooferland.showbiz.types.permissions.PermissionContext
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder

abstract class Command(val name: String) {
    abstract fun description(): MutableComponent?
    abstract fun run(ctx: CommandContext): Response

    open val showHelpOnRun: Boolean = false
    open val children: Subcommands = Subcommands()
    open fun requires(src: CommandSource): Boolean = true
    open fun checkPermission(perms: PermissionContext): Boolean = true
    open fun help(): MutableComponent? =
        usage().copy()
            .also { comp -> description()?.let { comp.append("\n  ").append(it.withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)) } }

    var parent: Command? = null
    val args = CommandArgument.Registry(this)

    fun usage(): Component {
        val words = mutableListOf<Component>()

        // Getting all parent commands
        val names = mutableListOf<String>()
        var command: Command? = this
        while (command != null) {
            names += command.name
            command = command.parent
        }
        words.addAll(names.reversed().map { Component.literal(it) })

        // Adding arguments
        for (arg in args.list) {
            words += Component.literal("<${arg.name}>").withStyle(ChatFormatting.GRAY)
        }

        // Creating the component
        val comp = Component.literal("/")
        for ((i, word) in words.withIndex()) {
            comp.append(word)
            if (i != words.size) comp.append(" ")
        }
        return comp
    }

    fun build(): LiteralArgumentBuilder<CommandSourceStack> {
        // Arguments
        var last: ArgumentBuilder<CommandSourceStack, *>? = null
        for (arg in args.list.reversed()) {
            val node = Commands.argument(arg.name, arg.type)
            node.requires { this.checkRequires(it) }
            node.executes { this.tryRun(CommandContext(it)) }
            arg.suggest?.let { block ->
                node.suggests { ctx, builder -> block(CommandContext(ctx), builder) }
            }
            last?.let { node.then(it) }
            last = node
        }

        // Command
        val required = args.list.count { !it.isOptional }
        val base = Commands.literal(name)
            .requires { this.checkRequires(it) }
            .executes { ctx ->
                val ctx = CommandContext(ctx)
                if (required == 0) {
                    this.tryRun(ctx)
                } else {
                    ctx.inner.source.sendSuccess({ help() }, false)
                    Response.ExitCodes.SUCCESS
                }
            }
        last?.let { base.then(it) }
        children.commands.forEach { base.then(it.build()) }
        return base
    }

    private fun checkRequires(ctx: CommandSourceStack): Boolean {
        // parent-ception
        var parentsAgree = true
        var parent: Command? = this.parent
        while (parent != null && parentsAgree) {
            if (!parent.checkRequires(ctx)) {
                parentsAgree = false
            }
            parent = parent.parent
        }

        val perms = ctx.player?.let { PermissionContext(ctx.server, it) }
        val source = CommandSource(ctx)
        return this.requires(source) && (perms?.let { this.checkPermission(it) } ?: true) && parentsAgree
    }

    private fun tryRun(ctx: CommandContext): Int {
        if (this.showHelpOnRun) {
            ctx.inner.source.sendSuccess({ help() }, false)
            return Response.ExitCodes.SUCCESS
        }

        try {
            val response = this.run(ctx)
            when (response.type) {
                Response.Type.Success -> ctx.inner.source.sendSuccess({ response.text }, false)
                Response.Type.Failure -> ctx.inner.source.sendFailure(response.text)
                Response.Type.None -> {}
            }
            return response.exitCode
        } catch (e: Exception) {
            ctx.inner.source.sendFailure(Component.literal("Unknown error running command:\n").append(e.toString()))
            return Response.ExitCodes.FAILURE
        }
    }

    inner class Subcommands(vararg val commands: Command) {
        init {
            for (command in commands) command.parent = this@Command
        }
    }

    data class Response(val text: Component, val type: Type, val exitCode: Int) {
        enum class Type { Success, Failure, None }
        object ExitCodes {
            const val SUCCESS = 1
            const val FAILURE = 1
        }
        companion object {
            fun empty() = Response(Component.empty(), Type.None, ExitCodes.SUCCESS)

            fun success(text: Component) = Response(text, Type.Success, ExitCodes.SUCCESS)
            fun success(text: String) = success(Component.literal(text))

            fun failure(text: Component) = Response(text, Type.Failure, ExitCodes.FAILURE)
            fun failure(text: String) = failure(Component.literal(text))
        }
    }
}