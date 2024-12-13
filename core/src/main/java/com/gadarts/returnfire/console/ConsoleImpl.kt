package com.gadarts.returnfire.console

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldFilter
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.gadarts.returnfire.DamagedTerritory
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.console.ConsoleConstants.TEXT_VIEW_NAME
import com.gadarts.returnfire.console.commands.CommandInvoke
import com.gadarts.returnfire.console.commands.ExecutedCommand
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.systems.events.SystemEvents
import java.util.*
import java.util.function.Consumer


class ConsoleImpl(assetsManager: GameAssetManager, private val dispatcher: MessageDispatcher) : Table(), Console,
    InputProcessor {
    private val consoleTextData: ConsoleTextData = ConsoleTextData(assetsManager)
    private val scrollPane by lazy {
        createScrollPane()
    }
    private val textWindowStack: Stack by lazy { Stack(scrollPane) }
    private val consoleTextures: ConsoleTextures = ConsoleTextures(calculateHeight())

    private fun calculateHeight() = (Gdx.graphics.height / 3F).toInt()
    private var textBackgroundTextureRegionDrawable: TextureRegionDrawable =
        TextureRegionDrawable(consoleTextures.textBackgroundTexture)
    private val consoleCommandResult = ConsoleCommandResult()

    private val arrow: Image by lazy {
        createArrow()
    }

    init {
        debug(if (GameDebugSettings.UI_DEBUG) Debug.all else Debug.none)
    }

    override val isActive: Boolean
        get() = active

    private var scrollToEnd: Boolean = false


    private val stringBuilder = StringBuilder()


    private val consoleInputHistoryHandler: ConsoleInputHistoryHandler by lazy { ConsoleInputHistoryHandler(stage) }
    private val input: TextField by lazy { addInputField(textBackgroundTextureRegionDrawable) }

    private var active = false
    override fun setStage(stage: Stage?) {
        super.setStage(stage)
        if (stage == null) return

        consoleTextData.stage = stage
        add(textWindowStack).colspan(2).size(width, height).align(Align.bottomLeft).padRight(PADDING)
            .padLeft(PADDING).row()
        input.textFieldFilter = TextFieldFilter { _, c ->
            if (c == '\t') return@TextFieldFilter false
            true
        }
        defineInputFieldTextFieldListener()
        defineInputFieldListener()
        insertNewLog("Damaged Territory v${DamagedTerritory.VERSION}", false)
    }

    private fun defineInputFieldListener() {
        input.addCaptureListener(object : InputListener() {
            override fun keyDown(event: InputEvent, keycode: Int): Boolean {
                var result = false
                if (active) {
                    result = true
                    when (keycode) {
                        Input.Keys.PAGE_UP -> scroll(-consoleTextData.fontHeight * 2)
                        Input.Keys.PAGE_DOWN -> scroll(consoleTextData.fontHeight * 2)
                        Input.Keys.TAB -> tryFindingCommand()
                        Input.Keys.ESCAPE -> deactivate()
                        else -> consoleInputHistoryHandler.onKeyDown(keycode)
                    }
                }
                return result
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

            private fun logSuggestedCommands(options: List<CommandList>) {
                stringBuilder.clear()
                options.forEach(Consumer { command: CommandList ->
                    stringBuilder.append(
                        command.name.lowercase(Locale.getDefault())
                    ).append(" | ")
                })
                insertNewLog(String.format("Possible options:\n%s", stringBuilder), false)
            }

            private fun scroll(step: Float) {
                scrollPane.scrollY += step
                scrollToEnd = false
            }
        })
    }

    private fun defineInputFieldTextFieldListener() {
        input.setTextFieldListener { textField: TextField, c: Char ->
            if (c == '`') {
                textField.text = null
                if (!this@ConsoleImpl.hasActions()) if (active) deactivate()
            }
            if (active) {
                if (c == '\r' || c == '\n') {
                    applyInput(input)
                }
            }
        }
    }

    private fun createScrollPane(): ScrollPane {
        val textStyle: LabelStyle = consoleTextData.textStyle
        textStyle.background = textBackgroundTextureRegionDrawable
        val textView = Label(consoleTextData.stringBuilder, textStyle)
        textView.setAlignment(Align.bottomLeft)
        textView.name = TEXT_VIEW_NAME
        textView.wrap = true
        val scrollPane = ScrollPane(textView)
        scrollPane.touchable = Touchable.disabled
        stage.addActor(scrollPane)
        return scrollPane
    }

    override fun activate() {
        if (active || !actions.isEmpty) return
        stage.setKeyboardFocus(stage.root.findActor(INPUT_FIELD_NAME))
        active = true
        val amountY = -Gdx.graphics.height / 3f
        addAction(Actions.moveBy(0f, amountY, TRANSITION_DURATION, Interpolation.pow2))
        isVisible = true
    }

    override fun deactivate() {
        if (!active || !actions.isEmpty) return
        active = false
        val amountY = Gdx.graphics.height / 3f
        val move = Actions.moveBy(0f, amountY, TRANSITION_DURATION, Interpolation.pow2)
        addAction(Actions.sequence(move, Actions.visible(false)))
        stage.unfocusAll()
    }

    override fun insertNewLog(text: String?, logTime: Boolean, color: String?) {
        if (text == null) return
        consoleTextData.insertNewLog(text, logTime, color)
        scrollToEnd = true
        arrow.isVisible = false
    }

    override fun notifyCommandExecution(
        command: Command,
        parameters: Map<String, String>
    ): ConsoleCommandResult {
        val result = false
        consoleCommandResult.clear()
        dispatcher.dispatchMessage(
            dispatcher,
            SystemEvents.CONSOLE_COMMAND_EXECUTED.ordinal,
            ExecutedCommand(command, parameters)
        )
        consoleCommandResult.result = result
        return consoleCommandResult
    }

    private fun createArrow(): Image {
        val arrow = Image(consoleTextures.arrowTexture)
        arrow.align = Align.bottomRight
        textWindowStack.add(arrow)
        arrow.setScaling(Scaling.none)
        arrow.setFillParent(false)
        arrow.isVisible = false
        return arrow
    }

    init {
        name = NAME
        val screenHeight = Gdx.graphics.height
        isVisible = false
        setPosition(0f, screenHeight.toFloat())
        background = TextureRegionDrawable(consoleTextures.backgroundTexture)
        setSize(Gdx.graphics.width.toFloat(), consoleTextures.backgroundTexture.height.toFloat())
        val multiplexer = Gdx.input.inputProcessor as InputMultiplexer
        multiplexer.addProcessor(this)
    }

    private fun applyInput(textField: TextField) {
        insertNewLog(textField.text, true, "[YELLOW]")
        val inputCommand = textField.text
        consoleInputHistoryHandler.applyInput(inputCommand)
        try {
            val commandToInvoke: CommandInvoke = parseCommandFromInput(inputCommand)!!
            val result: ConsoleCommandResult =
                commandToInvoke.getCommand().commandImpl.run(this, commandToInvoke.getParameters())
            insertNewLog(result.message, false)
        } catch (e: InputParsingFailureException) {
            insertNewLog(e.message, false)
        }
        textField.text = null
    }

    override fun keyDown(key: Int): Boolean {
        var result = false
        if (key == Input.Keys.GRAVE) {
            if (!active) {
                activate()
            }
            result = true
        }
        return result
    }

    override fun keyUp(keycode: Int): Boolean {
        return false
    }

    override fun keyTyped(character: Char): Boolean {
        return false
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchCancelled(p0: Int, p1: Int, p2: Int, p3: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun scrolled(p0: Float, p1: Float): Boolean {
        return false
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

    private fun addInputField(textBackgroundTexture: TextureRegionDrawable): TextField {
        val style = TextFieldStyle(
            consoleTextData.font, Color.YELLOW, TextureRegionDrawable(consoleTextures.cursorTexture),
            null, textBackgroundTexture
        )
        val input = TextField("", style)
        input.name = INPUT_FIELD_NAME
        val arrow = Label(">", consoleTextData.textStyle)
        add(arrow).padBottom(PADDING).padLeft(PADDING).align(Align.left).size(30F, INPUT_HEIGHT)
        add(input).size(Gdx.graphics.width - PADDING * 2F, INPUT_HEIGHT)
            .padBottom(PADDING).align(Align.left).row()
        input.focusTraversal = false
        return input
    }

    companion object {
        private const val NAME: String = "console"
        private const val PADDING: Float = 10f
        private const val INPUT_HEIGHT: Float = 20f
        private const val INPUT_FIELD_NAME: String = "input"
        private const val TRANSITION_DURATION: Float = 0.5f
    }
}
