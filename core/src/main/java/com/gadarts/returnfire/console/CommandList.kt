package com.gadarts.returnfire.console

import com.gadarts.returnfire.console.commands.HelpCommand
import com.gadarts.returnfire.console.commands.SkipDrawingCommand
import java.util.*

enum class CommandList(
    val commandImpl: ConsoleCommandImpl,
    description: String,
    val alias: String? = null,
    val parameters: List<CommandParameter> = emptyList()
) : Command {

    PROFILER(ProfilerCommand(), "Toggles profiler and GL operations stats."),
    SKIP_DRAWING(
        SkipDrawingCommand(), "Toggles drawing skipping mode for given categories.", "skip_draw", listOf(
            SkipDrawingCommand.GroundParameter,
            SkipDrawingCommand.CharactersParameter,
            SkipDrawingCommand.EnvironmentParameter,
            SkipDrawingCommand.ShadowsParameter
        )
    ),
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

        fun findCommandByNameOrAlias(input: String): CommandList {
            var result: Optional<CommandList>
            try {
                result = Optional.of<CommandList>(CommandList.valueOf(input))
            } catch (e: IllegalArgumentException) {
                val values = entries.toTypedArray()
                result = Arrays.stream(values).filter { command: CommandList ->
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
