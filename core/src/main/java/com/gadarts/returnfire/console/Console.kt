package com.gadarts.returnfire.console

interface Console {
    fun insertNewLog(text: String?, logTime: Boolean, color: String? = null)

    fun notifyCommandExecution(
        command: Command,
        parameters: Map<String, String> = mapOf()
    ): ConsoleCommandResult

    fun activate()

    fun deactivate()

    val isActive: Boolean

    companion object {
        const val ERROR_COLOR: String = "[RED]"
    }

}
