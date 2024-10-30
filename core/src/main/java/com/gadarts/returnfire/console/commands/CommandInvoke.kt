package com.gadarts.returnfire.console.commands

import com.gadarts.returnfire.console.CommandsList
import java.util.HashMap

class CommandInvoke(command: CommandsList) {
    private val command: CommandsList = command
    private val parameters: MutableMap<String, String> = HashMap()

    fun getCommand(): CommandsList {
        return command
    }

    fun addParameter(parameter: String, value: String) {
        parameters[parameter] = value
    }

    fun getParameters(): Map<String, String> {
        return parameters
    }
}