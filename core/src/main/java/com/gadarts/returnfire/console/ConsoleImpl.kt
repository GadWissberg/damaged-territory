package com.gadarts.returnfire.console

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldListener
import com.badlogic.gdx.scenes.scene2d.ui.TextField.TextFieldStyle
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.gadarts.returnfire.assets.GameAssetManager
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors

class ConsoleImpl(assetsManager: GameAssetManager) : Table(), Console, InputProcessor {
    private val consoleTextData: ConsoleTextData = ConsoleTextData(assetsManager)
    private val consoleTextures: ConsoleTextures = ConsoleTextures()
    private val consoleInputHistoryHandler: ConsoleInputHistoryHandler by lazy { ConsoleInputHistoryHandler() }
    private var input: TextField = null
    private var active = false

    init {
        name = NAME
        val screenHeight = Gdx.graphics.height
        val height = screenHeight / 3f
        isVisible = false
        setPosition(0f, screenHeight.toFloat())
        consoleTextures.init(height.toInt())
        val textBackgroundTextureRegionDrawable: TextureRegionDrawable =
            TextureRegionDrawable(consoleTextures.textBackgroundTexture)
        addTextView(textBackgroundTextureRegionDrawable, height.toInt())
        addInputField(textBackgroundTextureRegionDrawable)
        setBackground(TextureRegionDrawable(consoleTextures.backgroundTexture))
        setSize(Gdx.graphics.width.toFloat(), consoleTextures.backgroundTexture.height.toFloat())
        val multiplexer = Gdx.input.inputProcessor as InputMultiplexer
        multiplexer.addProcessor(this)
        input.setTextFieldListener(TextFieldListener { textField: TextField, c: Char ->
            if (c == '`') {
                textField.text = null
                if (!this@ConsoleImpl.hasActions()) if (active) deactivate()
            }
            if (active) {
                if (c == '\r' || c == '\n') {
                    applyInput(input)
                }
            }
        })
        input.addCaptureListener(object : InputListener() {
            override fun keyDown(event: InputEvent, keycode: Int): Boolean {
                var result = false
                if (active) {
                    result = true
                    if (keycode == Input.Keys.PAGE_UP) scroll(-consoleTextData.getFontHeight() * 2)
                    else if (keycode == Input.Keys.PAGE_DOWN) scroll(consoleTextData.getFontHeight() * 2)
                    else if (keycode == Input.Keys.TAB) tryFindingCommand()
                    else if (keycode == Input.Keys.ESCAPE) deactivate()
                    else consoleInputHistoryHandler.onKeyDown(keycode)
                }
                return result
            }

            private fun tryFindingCommand() {
                if (input.text.isEmpty()) return
                val options: List<CommandsList> = Arrays.stream(CommandsList.values())
                    .filter { command -> command.name().startsWith(input.text.uppercase(Locale.getDefault())) }
                    .collect(Collectors.toList())
                if (options.size == 1) {
                    input.setText(options[0].name().toLowerCase())
                    input.setCursorPosition(input.text.length)
                } else if (options.size > 1) logSuggestedCommands(options)
            }

            private fun logSuggestedCommands(options: List<CommandsList>) {
                stringBuilder.clear()
                options.forEach(Consumer<CommandsList> { command: CommandsList ->
                    stringBuilder.append(
                        command.name().toLowerCase()
                    ).append(ConsoleImpl.OPTIONS_DELIMITER)
                })
                insertNewLog(kotlin.String.format(ConsoleImpl.MSG_SUGGESTED, stringBuilder), false)
            }

            private fun scroll(step: Float) {
                scrollPane.setScrollY(scrollPane.getScrollY() + step)
                scrollToEnd = false
            }
        })

    }

    private fun applyInput(textField: TextField) {
        insertNewLog(textField.text, true, ConsoleImpl.INPUT_COLOR)
        val inputCommand = textField.text
        consoleInputHistoryHandler.applyInput(inputCommand)
        try {
            val commandToInvoke: CommandInvoke
            commandToInvoke = parseCommandFromInput(inputCommand)
            val result: ConsoleCommandResult =
                commandToInvoke.getCommand().getCommandImpl().run(this, commandToInvoke.getParameters())
            insertNewLog(result.getMessage(), false)
        } catch (e: InputParsingFailureException) {
            insertNewLog(e.getMessage(), false)
        }
        textField.text = null
    }

    private fun addTextView(textBackgroundTexture: TextureRegionDrawable, consoleHeight: Int) {
        val textStyle: LabelStyle = consoleTextData.textStyle
        textStyle.background = textBackgroundTexture
        val width: Float = Gdx.graphics.width - PADDING * 2
        val height: Float = consoleHeight - (ConsoleImpl.INPUT_HEIGHT)
        val textView = Label(consoleTextData.getStringBuilder(), textStyle)
        textView.setAlignment(Align.bottomLeft)
        textView.name = ConsoleImpl.TEXT_VIEW_NAME
        textView.wrap = true
        scrollPane = ScrollPane(textView)
        scrollPane.setTouchable(Touchable.disabled)
        val textWindowStack = Stack(scrollPane)
        arrow = Image(consoleTextures.getArrowTexture())
        arrow.setAlign(Align.bottomRight)
        textWindowStack.add(arrow)
        arrow.setScaling(Scaling.none)
        arrow.setFillParent(false)
        arrow.setVisible(false)
        add<Stack>(textWindowStack).colspan(2).size(width, height).align(Align.bottomLeft).padRight(PADDING)
            .padLeft(PADDING).row()
    }

    private fun addInputField(textBackgroundTexture: TextureRegionDrawable) {
        val style = TextFieldStyle(
            consoleTextData.getFont(), Color.YELLOW, TextureRegionDrawable(consoleTextures.getCursorTexture()),
            null, textBackgroundTexture
        )
        input = TextField("", style)
        input.name = ConsoleImpl.INPUT_FIELD_NAME
        val arrow: Label = Label(ConsoleImpl.INPUT_SIGN, consoleTextData.getTextStyle())
        add<Label>(arrow).padBottom(PADDING).padLeft(PADDING)
            .size(10f, ConsoleImpl.INPUT_HEIGHT)
        add<TextField>(input).size(Gdx.graphics.width - PADDING * 3, ConsoleImpl.INPUT_HEIGHT)
            .padBottom(PADDING).padRight(PADDING).align(Align.left).row()
        input.focusTraversal = false
    }

    companion object {
        private const val NAME: String = "console"
        private const val PADDING: Float = 10f

    }
}
