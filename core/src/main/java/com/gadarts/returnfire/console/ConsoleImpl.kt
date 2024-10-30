package com.gadarts.returnfire.console

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.console.ConsoleConstants.TEXT_VIEW_NAME
import com.gadarts.returnfire.console.commands.CommandInvoke
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

class ConsoleImpl(assetsManager: GameAssetManager) : Table(), Console, InputProcessor {
    private val textWindowStack: Stack by lazy { Stack(scrollPane) }
    private val consoleTextures: ConsoleTextures = ConsoleTextures()
    var textBackgroundTextureRegionDrawable: TextureRegionDrawable =
        TextureRegionDrawable(consoleTextures.textBackgroundTexture)
    private val consoleCommandResult = ConsoleCommandResult()

    private val arrow: Image by lazy {
        createArrow()
    }

    override fun activate() {
        if (active || !actions.isEmpty) return
        stage.setKeyboardFocus(stage.root.findActor(INPUT_FIELD_NAME))
        active = true
        val amountY = -Gdx.graphics.height / 3f
        addAction(Actions.moveBy(0f, amountY, TRANSITION_DURATION, Interpolation.pow2))
        isVisible = true
//        subscribers.forEach(ConsoleEventsSubscriber::onConsoleActivated)
    }

    override fun deactivate() {
        if (!active || !actions.isEmpty) return
        active = false
        val amountY = Gdx.graphics.height / 3f
        val move = Actions.moveBy(0f, amountY, TRANSITION_DURATION, Interpolation.pow2)
        addAction(Actions.sequence(move, Actions.visible(false)))
//        subscribers.forEach(ConsoleEventsSubscriber::onConsoleDeactivated)
        stage.unfocusAll()
    }

    override val isActive: Boolean
        get() = active


    override fun notifyCommandExecution(command: Commands, commandParameter: CommandParameter?): ConsoleCommandResult {
        val result = false
        consoleCommandResult.clear()
        val optional = Optional.ofNullable(commandParameter)
//        for (sub in subscribers) {
//            result = if (optional.isPresent) {
//                result or sub.onCommandRun(command, consoleCommandResult, optional.get())
//            } else {
//                result or sub.onCommandRun(command, consoleCommandResult)
//            }
//        }
        consoleCommandResult.result = result
        return consoleCommandResult
    }

    override fun insertNewLog(text: String?, logTime: Boolean, color: String?) {
        if (text == null) return
        consoleTextData.insertNewLog(text, logTime, color)
        scrollToEnd = true
        arrow.isVisible = false
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

    private var scrollToEnd: Boolean = false
    private val scrollPane by lazy {
        createScrollPane()
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
        return scrollPane
    }

    private val stringBuilder = StringBuilder()
    private val consoleTextData: ConsoleTextData = ConsoleTextData(assetsManager)
    private val consoleInputHistoryHandler: ConsoleInputHistoryHandler by lazy { ConsoleInputHistoryHandler() }
    private val input: TextField by lazy { addInputField(textBackgroundTextureRegionDrawable) }
    private var active = false

    init {
        name = NAME
        val screenHeight = Gdx.graphics.height
        val height = screenHeight / 3f
        isVisible = false
        setPosition(0f, screenHeight.toFloat())
        consoleTextures.init(height.toInt())
        addTextView(height.toInt())

        background = TextureRegionDrawable(consoleTextures.backgroundTexture)
        setSize(Gdx.graphics.width.toFloat(), consoleTextures.backgroundTexture.height.toFloat())
        val multiplexer = Gdx.input.inputProcessor as InputMultiplexer
        multiplexer.addProcessor(this)
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
        input.addCaptureListener(object : InputListener() {
            override fun keyDown(event: InputEvent, keycode: Int): Boolean {
                var result = false
                if (active) {
                    result = true
                    if (keycode == Input.Keys.PAGE_UP) scroll(-consoleTextData.fontHeight * 2)
                    else if (keycode == Input.Keys.PAGE_DOWN) scroll(consoleTextData.fontHeight * 2)
                    else if (keycode == Input.Keys.TAB) tryFindingCommand()
                    else if (keycode == Input.Keys.ESCAPE) deactivate()
                    else consoleInputHistoryHandler.onKeyDown(keycode)
                }
                return result
            }

            private fun tryFindingCommand() {
                if (input.text.isEmpty()) return
                val options: List<CommandsList> = Arrays.stream(CommandsList.entries.toTypedArray())
                    .filter { command -> command.name.startsWith(input.text.uppercase(Locale.getDefault())) }
                    .collect(Collectors.toList())
                if (options.size == 1) {
                    input.text = options[0].name.lowercase(Locale.getDefault())
                    input.cursorPosition = input.text.length
                } else if (options.size > 1) logSuggestedCommands(options)
            }

            private fun logSuggestedCommands(options: List<CommandsList>) {
                stringBuilder.clear()
                options.forEach(Consumer<CommandsList> { command: CommandsList ->
                    stringBuilder.append(
                        command.name.lowercase(Locale.getDefault())
                    ).append(" | ")
                })
                insertNewLog(String.format("Possible options:\n%s", stringBuilder), false)
            }

            private fun scroll(step: Float) {
                scrollPane.scrollY = scrollPane.scrollY + step
                scrollToEnd = false
            }
        })

    }

    private fun applyInput(textField: TextField) {
        insertNewLog(textField.text, true, "[YELLOW]")
        val inputCommand = textField.text
        consoleInputHistoryHandler.applyInput(inputCommand)
        try {
            val commandToInvoke: CommandInvoke
            commandToInvoke = parseCommandFromInput(inputCommand)!!
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
        val command = CommandInvoke(CommandsList.findCommandByNameOrAlias(commandName))
        parseParameters(entries, command)
        return command
    }

    @Throws(InputParsingFailureException::class)
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

    private fun addTextView(consoleHeight: Int) {
        val width: Float = Gdx.graphics.width - PADDING * 2
        val height: Float = consoleHeight - (INPUT_HEIGHT)
        val textWindowStack = Stack(scrollPane)
        add<Stack>(textWindowStack).colspan(2).size(width, height).align(Align.bottomLeft).padRight(PADDING)
            .padLeft(PADDING).row()
    }

    private fun addInputField(textBackgroundTexture: TextureRegionDrawable): TextField {
        val style = TextFieldStyle(
            consoleTextData.font, Color.YELLOW, TextureRegionDrawable(consoleTextures.cursorTexture),
            null, textBackgroundTexture
        )
        val input = TextField("", style)
        input.name = INPUT_FIELD_NAME
        val arrow = Label(">", consoleTextData.textStyle)
        add<Label>(arrow).padBottom(PADDING).padLeft(PADDING)
            .size(10f, INPUT_HEIGHT)
        add<TextField>(input).size(Gdx.graphics.width - PADDING * 3, INPUT_HEIGHT)
            .padBottom(PADDING).padRight(PADDING).align(Align.left).row()
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
