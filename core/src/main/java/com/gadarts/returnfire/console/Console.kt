package com.gadarts.returnfire.console

interface Console {
    fun insertNewLog(var1: String?, var2: Boolean)

    fun insertNewLog(var1: String?, var2: Boolean, var3: String?)

    fun notifyCommandExecution(var1: Commands?): ConsoleCommandResult?

    fun notifyCommandExecution(var1: Commands?, var2: CommandParameter?): ConsoleCommandResult?

    fun activate()

    fun deactivate()

    val isActive: Boolean

    companion object {
        const val ERROR_COLOR: String = "[RED]"
    }

}
