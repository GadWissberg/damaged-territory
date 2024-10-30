package com.gadarts.returnfire.console

import com.badlogic.gdx.Input
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.Array
import com.gadarts.returnfire.console.ConsoleConstants.INPUT_FIELD_NAME
import kotlin.math.max
import kotlin.math.min

class ConsoleInputHistoryHandler {
    private var stage: Stage? = null
    private val inputHistory = Array<String>()
    private var current = 0

    fun applyInput(inputCommand: String) {
        inputHistory.insert(inputHistory.size, inputCommand)
        current = inputHistory.size
    }

    fun onKeyDown(keycode: Int) {
        if (keycode == Input.Keys.DOWN) {
            current = min((inputHistory.size - 1).toDouble(), (current + 1).toDouble()).toInt()
            updateInputByHistory()
        } else if (keycode == Input.Keys.UP) {
            current = max(0.0, (current - 1).toDouble()).toInt()
            updateInputByHistory()
        }
    }

    private fun updateInputByHistory() {
        if (inputHistory.isEmpty) return
        val input = stage!!.root.findActor<TextField>(INPUT_FIELD_NAME)
        input.text = inputHistory[current]
        input.cursorPosition = input.text.length
    }

    fun setStage(stage: Stage?) {
        this.stage = stage
    }
}
