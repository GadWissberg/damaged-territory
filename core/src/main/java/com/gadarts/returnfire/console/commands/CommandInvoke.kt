package com.gadarts.returnfire.console.commands

import com.gadarts.returnfire.console.CommandList

class CommandInvoke(private val command: CommandList) {
    private val parameters: MutableMap<String, String> = HashMap()

    fun getCommand(): CommandList {
        return command
    }

    fun addParameter(parameter: String, value: String) {
        parameters[parameter] = value
    }

    fun getParameters(): Map<String, String> {
        return parameters
    }
}
