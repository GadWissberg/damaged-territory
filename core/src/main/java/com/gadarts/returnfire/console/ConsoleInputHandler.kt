package com.gadarts.returnfire.console

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.gadarts.returnfire.console.commands.CommandInvoke
import java.util.*
import java.util.function.Consumer

class ConsoleInputHandler(
    private val console: Console,
    private val consoleOutputWindowHandler: ConsoleOutputWindowHandler,
    private val consoleTextures: ConsoleTextures,
    private val scrollHandler: ConsoleScrollHandler
) {
    private val stringBuilder = StringBuilder()
    private lateinit var consoleInputHistoryHandler: ConsoleInputHistoryHandler
    private lateinit var input: TextField

    fun init(stage: Stage) {
        input = addInputField(consoleTextures.textBackgroundTextureRegionDrawable)
        applyTextFiledFilter()
        defineInputFieldTextFieldListener()
        defineInputFieldListener()
        consoleInputHistoryHandler = ConsoleInputHistoryHandler(stage)
    }

    private fun addInputField(textBackgroundTexture: TextureRegionDrawable): TextField {
        val style = TextFieldStyle(
            consoleOutputWindowHandler.font, Color.YELLOW, TextureRegionDrawable(consoleTextures.cursorTexture),
            null, textBackgroundTexture
        )
        val input = TextField("", style)
        input.name = Console.INPUT_FIELD_NAME
        val arrow = Label(">", consoleOutputWindowHandler.textStyle)
        console.addUiWidget(arrow).padBottom(PADDING).padLeft(PADDING).align(Align.left).size(30F, INPUT_HEIGHT)
        console.addUiWidget(input).size(Gdx.graphics.width - PADDING * 2F, INPUT_HEIGHT)
            .padBottom(PADDING).align(Align.left).row()
        input.focusTraversal = false
        return input
    }

    private fun parseCommandFromInput(text: String?): CommandInvoke? {
        if (text == null) return null
        val entries =
            text.uppercase(Locale.getDefault()).split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val commandName = entries[0]
        val command = CommandInvoke(CommandList.findCommandByNameOrAlias(commandName))
        parseParameters(entries, command)
        return command
    }

    private fun parseParameters(entries: Array<String>, command: CommandInvoke) {
        var i = 1
        while (i < entries.size) {
            val parameter = entries[i]
            if (!parameter.startsWith("-") || parameter.length < 2) throw InputParsingFailureException(
                String.format(
                    "Failed to apply command! Parameter is expected at '%s'",
                    parameter
                )
            )
            if (i + 1 == entries.size || entries[i + 1].startsWith("-")) throw InputParsingFailureException(
                String.format(
                    "Failed to apply command! Value is expected for parameter '%s'",
                    parameter
                )
            )
            command.addParameter(parameter.substring(1), entries[i + 1])
            i += 2
        }
    }


    private fun logSuggestedCommands(options: List<CommandList>) {
        stringBuilder.clear()
        options.forEach(Consumer { command: CommandList ->
            stringBuilder.append(
                command.name.lowercase(Locale.getDefault())
            ).append(" | ")
        })
        console.insertNewLog(String.format("Possible options:\n%s", stringBuilder), false)
    }

    private fun applyInput(textField: TextField) {
        console.insertNewLog(textField.text, true, "[YELLOW]")
        val inputCommand = textField.text
        consoleInputHistoryHandler.applyInput(inputCommand)
        try {
            val commandToInvoke: CommandInvoke = parseCommandFromInput(inputCommand)!!
            val result: ConsoleCommandResult =
                commandToInvoke.getCommand().commandImpl.run(console, commandToInvoke.getParameters())
            console.insertNewLog(result.message, false)
        } catch (e: InputParsingFailureException) {
            console.insertNewLog(e.message, false)
        }
        textField.text = null
    }

    private fun defineInputFieldListener() {
        input.addCaptureListener(object : InputListener() {
            override fun keyDown(event: InputEvent, keycode: Int): Boolean {
                var result = false
                if (console.isActive()) {
                    result = true
                    applyKeyDown(keycode)
                }
                return result
            }
        })
    }

    private fun tryFindingCommand() {
        if (input.text.isEmpty()) return
        val options: List<CommandList> = CommandList.entries.toTypedArray()
            .filter { command -> command.name.startsWith(input.text.uppercase()) }
        if (options.size == 1) {
            input.text = options[0].name.lowercase()
            input.cursorPosition = input.text.length
        } else if (options.size > 1) logSuggestedCommands(options)
    }

    private fun applyKeyDown(keycode: Int) {
        when (keycode) {
            Input.Keys.PAGE_UP -> scrollHandler.scroll(-consoleOutputWindowHandler.fontHeight * 2)
            Input.Keys.PAGE_DOWN -> scrollHandler.scroll(consoleOutputWindowHandler.fontHeight * 2)
            Input.Keys.TAB -> tryFindingCommand()
            Input.Keys.ESCAPE -> console.deactivate()
            else -> consoleInputHistoryHandler.onKeyDown(keycode)
        }
    }

    private fun defineInputFieldTextFieldListener() {
        input.setTextFieldListener { textField: TextField, c: Char ->
            if (c == '`') {
                textField.text = null
                if (!console.hasWidgetActions()) if (console.isActive()) console.deactivate()
            }
            if (console.isActive()) {
                if (c == '\r' || c == '\n') {
                    applyInput(input)
                }
            }
        }
    }

    private fun applyTextFiledFilter() {
        input.textFieldFilter = TextField.TextFieldFilter { _, c ->
            if (c == '\t') return@TextFieldFilter false
            true
        }
    }

    companion object {
        private const val INPUT_HEIGHT: Float = 20f
        private const val PADDING: Float = 10f
    }
}
