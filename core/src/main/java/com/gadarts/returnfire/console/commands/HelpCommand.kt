package com.gadarts.returnfire.console.commands

import com.badlogic.gdx.utils.StringBuilder
import com.gadarts.returnfire.DamagedTerritory
import com.gadarts.returnfire.console.CommandList
import com.gadarts.returnfire.console.Console
import com.gadarts.returnfire.console.ConsoleCommandImpl
import com.gadarts.returnfire.console.ConsoleCommandResult
import java.util.*

class HelpCommand : ConsoleCommandImpl() {
    override val commandEnumValue: CommandList
        get() = CommandList.HELP

    override fun run(console: Console, parameters: Map<String, String>): ConsoleCommandResult {
        if (!Optional.ofNullable(output).isPresent) {
            initializeMessage()
        }
        val consoleCommandResult = ConsoleCommandResult()
        consoleCommandResult.message = output
        return consoleCommandResult
    }

    private fun initializeMessage() {
        val builder = StringBuilder()
        Arrays.stream(CommandList.entries.toTypedArray()).forEach { command ->
            builder.append(" - ").append(command.name.lowercase(Locale.getDefault()))
            if (command.alias != null) {
                builder.append(" (also '").append(command.alias).append("')")
            }
            builder.append(": ").append(command.description).append("\n")
        }
        output = java.lang.String.format(INTRO, DamagedTerritory.VERSION, builder)
    }

    companion object {
        private const val INTRO = "Welcome to Damaged Territory v%s. The command pattern is '<COMMAND_NAME> " +
            "-<PARAMETER_1> <PARAMETER_1_VALUE>'. The following commands are available:\n%s"

        private var output: String? = null
    }
}
