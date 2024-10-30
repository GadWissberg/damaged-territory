package com.gadarts.returnfire.console

abstract class CommandParameter(val description: String, val alias: String) {
    var parameterValue: Boolean = false

    fun defineParameterValue(
        value: String,
        console: Console,
        messageOnParameterActivation: String?,
        messageOnParameterDeactivation: String?
    ): Boolean {
        var result = try {
            value.toInt() == 1
        } catch (e: NumberFormatException) {
            false
        }
        val msg = String.format(
            (if (result) messageOnParameterActivation else messageOnParameterDeactivation)!!,
            alias
        )
        console.insertNewLog(msg, false)
        parameterValue = result
        return result
    }

    abstract fun run(value: String?, console: Console?)
}
