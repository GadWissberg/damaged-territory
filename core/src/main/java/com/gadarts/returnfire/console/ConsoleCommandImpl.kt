package com.gadarts.returnfire.console

import java.util.stream.IntStream


abstract class ConsoleCommandImpl {
    private val stringBuilder = StringBuilder()

    open fun run(console: Console, parameters: Map<String, String>): ConsoleCommandResult {
        val result = console.notifyCommandExecution(commandEnumValue)
        if (!parameters.isEmpty()) {
            parameters.forEach { (key: String?, value: String?) ->
                commandEnumValue.parameters.forEach { parameter ->
                    if (key.equals(parameter.alias, ignoreCase = true)) {
                        parameter.run(value, console)
                    }
                }
            }
        } else {
            printNoParameters(result)
        }
        return result
    }

    private fun printNoParameters(result: ConsoleCommandResult) {
        val length: Int = commandEnumValue.parameters.size
        IntStream.range(0, length).forEach { i: Int ->
            stringBuilder.append(commandEnumValue.parameters.get(i).alias)
            if (i < length - 1) {
                stringBuilder.append(", ")
            }
        }
        result.message = String.format(NO_PARAMETERS, stringBuilder)
    }

    protected abstract val commandEnumValue: CommandsList

    companion object {
        private const val NO_PARAMETERS = "No parameters were supplied. Please supply one or more of the following: %s"
    }
}