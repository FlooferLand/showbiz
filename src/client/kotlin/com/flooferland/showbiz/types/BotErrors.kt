package com.flooferland.showbiz.types

object BotErrors {
    private val errorsTriggered = mutableSetOf<BotError>()

    fun isEmpty() = errorsTriggered.isEmpty()
    fun isNotEmpty() = errorsTriggered.isNotEmpty()

    fun add(err: BotError) {
        if (errorsTriggered.any { it.bot?.equals(err.bot) == true || it.botId?.equals(err.bot) == true }) return
        errorsTriggered += err
    }
    fun get(bot: IBot): Collection<BotError> {
        return errorsTriggered.filter { it.botId == bot.botId.toString() }
    }
    fun getAll(): Collection<BotError> {
        return errorsTriggered
    }
    fun clear() {
        errorsTriggered.clear()
    }
}