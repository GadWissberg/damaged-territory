package com.gadarts.returnfire.console.commands

import com.gadarts.returnfire.console.CommandList
import com.gadarts.returnfire.console.CommandParameter
import com.gadarts.returnfire.console.ConsoleCommandImpl


class SkipDrawingCommand : ConsoleCommandImpl() {
    override val commandEnumValue: CommandList
        get() = CommandList.SKIP_DRAWING

    object GroundParameter : SkipDrawingParameter("0 - Draws ground. 1 - Skips.", "ground")

    object CharactersParameter : SkipDrawingParameter("0 - Draws characters. 1 - Skips.", "characters")

    object EnvironmentParameter : SkipDrawingParameter("0 - Draws environment objects. 1 - Skips.", "environment")

    object ShadowsParameter : SkipDrawingParameter("0 - Draws shadows. 1 - Skips.", "shadows")

    abstract class SkipDrawingParameter(description: String, alias: String) :
        CommandParameter(description, alias)

}
