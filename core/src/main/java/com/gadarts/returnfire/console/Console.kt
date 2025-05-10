package com.gadarts.returnfire.console

import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Cell

interface Console {
    fun insertNewLog(text: String?, logTime: Boolean, color: String? = null)

    fun notifyCommandExecution(
        command: Command,
        parameters: Map<String, String> = mapOf()
    ): ConsoleCommandResult

    fun activate()

    fun deactivate()

    fun isActive(): Boolean
    fun addUiWidget(actor: Actor): Cell<out Actor>
    fun hasWidgetActions(): Boolean

    companion object {
        const val INPUT_FIELD_NAME: String = "input"

    }
}
