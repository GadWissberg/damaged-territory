@file:Suppress("KotlinConstantConditions")

package com.gadarts.returnfire.systems.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.model.TurretCharacterDefinition
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class HudSystem : GameEntitySystem() {
    private var onboardButton: ImageButton? = null
    private val debugInput: CameraInputController by lazy { CameraInputController(gameSessionData.renderData.camera) }


    private val priWeaponButtonClickListener = object : ClickListener() {
        override fun touchDown(
            event: InputEvent,
            x: Float,
            y: Float,
            pointer: Int,
            button: Int
        ): Boolean {
            managers.dispatcher.dispatchMessage(SystemEvents.BUTTON_WEAPON_PRIMARY_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
            managers.dispatcher.dispatchMessage(SystemEvents.BUTTON_WEAPON_PRIMARY_RELEASED.ordinal)
            super.touchUp(event, x, y, pointer, button)
        }
    }

    private val secWeaponButtonClickListener = object : ClickListener() {
        override fun touchDown(
            event: InputEvent,
            x: Float,
            y: Float,
            pointer: Int,
            button: Int
        ): Boolean {
            managers.dispatcher.dispatchMessage(SystemEvents.BUTTON_WEAPON_SECONDARY_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
            managers.dispatcher.dispatchMessage(SystemEvents.BUTTON_WEAPON_SECONDARY_RELEASED.ordinal)
            super.touchUp(event, x, y, pointer, button)
        }
    }

    private val reverseButtonClickListener = object : ClickListener() {
        override fun touchDown(
            event: InputEvent,
            x: Float,
            y: Float,
            pointer: Int,
            button: Int
        ): Boolean {
            managers.dispatcher.dispatchMessage(SystemEvents.BUTTON_REVERSE_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
            managers.dispatcher.dispatchMessage(SystemEvents.BUTTON_REVERSE_RELEASED.ordinal)
            super.touchUp(event, x, y, pointer, button)
        }
    }
    private val onBoardButtonClickListener = object : ClickListener() {
        override fun touchDown(
            event: InputEvent,
            x: Float,
            y: Float,
            pointer: Int,
            button: Int
        ): Boolean {
            managers.dispatcher.dispatchMessage(SystemEvents.BUTTON_ONBOARD_PRESSED.ordinal)
            if (onboardButton != null) {
                onboardButton!!.isVisible = false
            }
            return super.touchDown(event, x, y, pointer, button)
        }
    }

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        val ui = addUiTable()
        addOnScreenInput(gameSessionData, ui)
        initializeInput()
        val console = gameSessionData.hudData.console
        gameSessionData.hudData.stage.addActor(console)
        console.toFront()
    }

    private fun addOnScreenInput(
        gameSessionData: GameSessionData,
        ui: Table
    ) {
        if (gameSessionData.runsOnMobile) {
            val movementPad =
                addTouchpad(ui, this.gameSessionData.hudData.movementTouchpad)
                    .pad(0F, JOYSTICK_PADDING, JOYSTICK_PADDING, 0F).left()
            val definition = ComponentsMapper.character.get(gameSessionData.gameplayData.player).definition
            if (definition == SimpleCharacterDefinition.APACHE) {
                movementPad.growX()
                addApacheButtons(ui)
            } else if (definition == TurretCharacterDefinition.TANK) {
                val touchpad = this.gameSessionData.hudData.turretTouchpad
                val imageButtonCell = addButton(
                    ui,
                    "icon_reverse",
                    reverseButtonClickListener
                ).size(150F)
                imageButtonCell.growX().left().bottom().padBottom(JOYSTICK_PADDING)
                addTouchpad(ui, touchpad).padRight(JOYSTICK_PADDING).padBottom(JOYSTICK_PADDING)
                    .right()
            }
            val cell = addButton(
                ui,
                "icon_reverse",
                onBoardButtonClickListener,
                visible = false
            ).size(150F)
            onboardButton = cell.actor
            cell.left().bottom().padBottom(JOYSTICK_PADDING)
        }
    }

    private fun addApacheButtons(ui: Table) {
        addButton(
            ui,
            "icon_bullets",
            clickListener = priWeaponButtonClickListener
        )
        addButton(
            ui,
            "icon_missiles",
            secWeaponButtonClickListener,
            JOYSTICK_PADDING,
        )
    }

    private fun addButton(
        ui: Table,
        iconDefinition: String,
        clickListener: ClickListener,
        rightPadding: Float = 0F,
        visible: Boolean = true
    ): Cell<ImageButton> {
        val up = TextureRegionDrawable(managers.assetsManager.getTexture("button_up"))
        val down =
            TextureRegionDrawable(managers.assetsManager.getTexture("button_down"))
        val icon =
            TextureRegionDrawable(managers.assetsManager.getTexture(iconDefinition))
        val button = ImageButton(ImageButton.ImageButtonStyle(up, down, null, icon, null, null))
        val cell = ui.add(button)
        if (rightPadding != 0F) {
            ui.pad(0F, 0F, 0F, rightPadding)
        }
        button.addListener(clickListener)
        button.isVisible = visible
        return cell
    }

    private fun addTouchpad(ui: Table, touchpad: Touchpad): Cell<Touchpad> {
        val joystickTexture = managers.assetsManager.getTexture("joystick")
        return ui.add(touchpad)
            .size(joystickTexture.width.toFloat(), joystickTexture.height.toFloat())
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.LANDING_INDICATOR_VISIBILITY_CHANGED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                managers: Managers
            ) {
                if (onboardButton != null) {
                    onboardButton!!.isVisible = msg.extraInfo as Boolean
                }
            }
        }
    )

    override fun resume(delta: Long) {

    }

    override fun dispose() {
    }


    private fun initializeInput() {
        if (GameDebugSettings.DEBUG_INPUT) {
            debugInput.autoUpdate = true
            Gdx.input.inputProcessor = debugInput
        } else {
            (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(gameSessionData.hudData.stage)
        }
    }

    private fun addUiTable(): Table {
        val uiTable = Table()
        uiTable.debug(if (GameDebugSettings.UI_DEBUG) Table.Debug.all else Table.Debug.none)
        uiTable.name = GameSessionData.UI_TABLE_NAME
        uiTable.setFillParent(true)
        uiTable.setSize(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        uiTable.align(Align.bottom)
        gameSessionData.hudData.stage.addActor(uiTable)
        return uiTable
    }

    override fun update(deltaTime: Float) {
        if (gameSessionData.gameplayData.sessionFinished) return

        if (GameDebugSettings.DEBUG_INPUT) {
            debugInput.update()
        }
        if (!GameDebugSettings.DISABLE_HUD) {
            gameSessionData.hudData.stage.act(deltaTime)
            gameSessionData.hudData.stage.draw()
        }
    }

    companion object {
        private const val JOYSTICK_PADDING = 64F
    }
}
