package com.gadarts.returnfire.console

class ProfilerCommand : ConsoleCommandImpl() {
    override fun run(console: Console, parameters: Map<String, String>): ConsoleCommandResult {
        val result = super.run(console, parameters)
        return result
    }

    override val commandEnumValue: CommandList
        get() = CommandList.PROFILER

}
