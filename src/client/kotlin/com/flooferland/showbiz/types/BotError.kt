package com.flooferland.showbiz.types

import net.minecraft.world.phys.Vec3

sealed class BotError {
    class MissingBot : BotError()
    class MissingModel : BotError()
    class MissingTexture : BotError()
    class MissingAnimation : BotError()
    class RenderException : BotError()

    val name: String = this.javaClass.simpleName

    var bot: IBot? = null
    var context: String? = null
    var botId: String? = null
    var botPos: Vec3? = null
    fun withContext(context: Throwable): BotError {
        this.context = context.toString()
        return this
    }
    fun withContext(context: String): BotError {
        this.context = context
        return this
    }
    fun withBot(animatable: IBot): BotError {
        this.bot = animatable
        this.botId = animatable.botId.toString()
        this.botPos = animatable.botPos
        return this
    }
}
