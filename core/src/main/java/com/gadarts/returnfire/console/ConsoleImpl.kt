package com.gadarts.returnfire.console

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.gadarts.returnfire.DamagedTerritory
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.console.ConsoleConstants.INPUT_FIELD_NAME
import com.gadarts.returnfire.console.commands.ExecutedCommand
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.systems.events.SystemEvents


class ConsoleImpl(assetsManager: GameAssetManager, private val dispatcher: MessageDispatcher) : Table(), Console,
    InputProcessor {
    private val consoleTextures: ConsoleTextures = ConsoleTextures(calculateHeight())
    private val consoleOutputWindowHandler: ConsoleOutputWindowHandler =
        ConsoleOutputWindowHandler(this, consoleTextures, assetsManager)
    private val scrollHandler = ConsoleScrollHandler(consoleOutputWindowHandler, consoleTextures)
    private val consoleCommandResult = ConsoleCommandResult()
    private val inputHandler =
        ConsoleInputHandler(this, consoleOutputWindowHandler, consoleTextures, scrollHandler)
    private var active = false

    init {
        debug(if (GameDebugSettings.UI_DEBUG) Debug.all else Debug.none)
    }

    private fun calculateHeight(): Int {
        return (Gdx.graphics.height / 3F).toInt()
    }

    override fun setStage(stage: Stage?) {
        super.setStage(stage)
        if (stage == null) return

        scrollHandler.init(stage)
        consoleOutputWindowHandler.init(scrollHandler, width, height, stage)
        inputHandler.init(stage)
        insertNewLog("Damaged Territory v${DamagedTerritory.VERSION}", false)
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

    override fun isActive(): Boolean {
        return active
    }

    override fun hasWidgetActions(): Boolean {
        return hasActions()
    }

    override fun addUiWidget(actor: Actor): Cell<out Actor> {
        return add(actor)
    }


    override fun insertNewLog(text: String?, logTime: Boolean, color: String?) {
        if (text == null) return
        consoleOutputWindowHandler.insertNewLog(text, logTime, color)
        scrollHandler.scrollToEnd()
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


    companion object {
        private const val NAME: String = "console"
        private const val TRANSITION_DURATION: Float = 0.5f
    }
}
