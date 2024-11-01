package com.gadarts.returnfire.console

import java.util.stream.IntStream


abstract class ConsoleCommandImpl {
    private val stringBuilder = StringBuilder()

    open fun run(console: Console, parameters: Map<String, String>): ConsoleCommandResult {
        val result = ConsoleCommandResult()
        if (parameters.isNotEmpty()) {
            console.notifyCommandExecution(commandEnumValue, parameters)
        } else if (commandEnumValue.parameters.isNotEmpty()) {
            printNoParameters(result)
        } else {
            console.notifyCommandExecution(commandEnumValue)
        }
        return result
    }

    private fun printNoParameters(result: ConsoleCommandResult) {
        val length: Int = commandEnumValue.parameters.size
        IntStream.range(0, length).forEach { i: Int ->
            stringBuilder.append(commandEnumValue.parameters[i].alias)
            if (i < length - 1) {
                stringBuilder.append(", ")
            }
        }
        result.message = String.format(NO_PARAMETERS, stringBuilder)
    }

    protected abstract val commandEnumValue: CommandList

    companion object {
        private const val NO_PARAMETERS = "No parameters were supplied. Please supply one or more of the following: %s"
    }
}
