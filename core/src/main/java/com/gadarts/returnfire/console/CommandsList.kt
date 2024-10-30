package com.gadarts.returnfire.console

import com.gadarts.returnfire.console.commands.HelpCommand
import java.util.*

enum class CommandsList(
    val commandImpl: ConsoleCommandImpl,
    description: String,
    val alias: String? = null,
    val parameters: List<CommandParameter> = emptyList()
) : Commands {

    PROFILER(ProfilerCommand(), "Toggles profiler and GL operations stats."),
    HELP(HelpCommand(), "Displays commands list.", "?");

    var description: String
        private set

    init {
        this.description = description
    }

    private fun extendDescriptionWithParameters() {
        if (parameters.size > 0) {
            val stringBuilder = StringBuilder()
            parameters.forEach { parameter ->
                stringBuilder
                    .append("\n")
                    .append("   * ")
                    .append(parameter.alias)
                    .append(": ")
                    .append(parameter.description)
            }
            this.description += String.format(DESCRIPTION_PARAMETERS, stringBuilder)
        }
    }

    companion object {
        const val DESCRIPTION_PARAMETERS: String = " Parameters:%s"

        fun findCommandByNameOrAlias(input: String): CommandsList {
            var result: Optional<CommandsList>
            try {
                result = Optional.of<CommandsList>(CommandsList.valueOf(input))
            } catch (e: IllegalArgumentException) {
                val values = entries.toTypedArray()
                result = Arrays.stream(values).filter { command: CommandsList ->
                    Optional.ofNullable(
                        command.alias
                    ).isPresent &&
                            command.alias.equals(input, ignoreCase = true)
                }.findFirst()
                if (!result.isPresent) {
                    throw InputParsingFailureException(
                        java.lang.String.format(
                            "'%s' is not recognized as a command.",
                            input.lowercase(Locale.getDefault())
                        )
                    )
                }
            }
            return result.get()
        }
    }
}