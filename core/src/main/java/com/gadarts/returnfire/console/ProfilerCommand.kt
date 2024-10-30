package com.gadarts.returnfire.console

class ProfilerCommand : ConsoleCommandImpl() {
    override fun run(console: Console, parameters: Map<String, String>): ConsoleCommandResult {
        val result = super.run(console, parameters)
        return result
    }

    override val commandEnumValue: CommandsList
        get() = CommandsList.PROFILER

    companion object {
        const val PROFILING_ACTIVATED: String = "Profiling info is displayed."
        const val PROFILING_DEACTIVATED: String = "Profiling info is hidden."
    }
}