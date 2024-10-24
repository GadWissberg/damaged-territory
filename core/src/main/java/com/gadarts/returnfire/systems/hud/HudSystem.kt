@file:Suppress("KotlinConstantConditions")

package com.gadarts.returnfire.systems.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
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

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        val ui = addUiTable()
        addOnScreenInput(gameSessionData, ui)
        initializeInput()
    }

    private fun addOnScreenInput(
        gameSessionData: GameSessionData,
        ui: Table
    ) {
        if (gameSessionData.runsOnMobile) {
            val movementPad = addTouchpad(ui, this.gameSessionData.gameSessionDataHud.movementTouchpad)
                .pad(0F, JOYSTICK_PADDING, JOYSTICK_PADDING, 0F).left()
            val definition = ComponentsMapper.character.get(gameSessionData.player).definition
            if (definition == SimpleCharacterDefinition.APACHE) {
                movementPad.growX()
                addApacheButtons(ui)
            } else if (definition == TurretCharacterDefinition.TANK) {
                val touchpad = this.gameSessionData.gameSessionDataHud.turretTouchpad
                val imageButtonCell = addButton(
                    ui,
                    "icon_reverse",
                    reverseButtonClickListener
                ).size(150F)
                imageButtonCell.growX().left().bottom().padBottom(JOYSTICK_PADDING)
                addTouchpad(ui, touchpad).padRight(JOYSTICK_PADDING).padBottom(JOYSTICK_PADDING)
                    .right()
            }
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
        return cell
    }

    private fun addTouchpad(ui: Table, touchpad: Touchpad): Cell<Touchpad> {
        val joystickTexture = managers.assetsManager.getTexture("joystick")
        return ui.add(touchpad)
            .size(joystickTexture.width.toFloat(), joystickTexture.height.toFloat())
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    override fun resume(delta: Long) {

    }

    override fun dispose() {
    }


    private fun initializeInput() {
        if (GameDebugSettings.DEBUG_INPUT) {
            debugInput.autoUpdate = true
            Gdx.input.inputProcessor = debugInput
        } else {
            (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(gameSessionData.gameSessionDataHud.stage)
        }
    }

    private fun addUiTable(): Table {
        val uiTable = Table()
        uiTable.debug(if (GameDebugSettings.UI_DEBUG) Table.Debug.all else Table.Debug.none)
        uiTable.name = GameSessionData.UI_TABLE_NAME
        uiTable.setFillParent(true)
        uiTable.setSize(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        uiTable.align(Align.bottom)
        gameSessionData.gameSessionDataHud.stage.addActor(uiTable)
        return uiTable
    }

    override fun update(deltaTime: Float) {
        if (gameSessionData.sessionFinished) return

        if (GameDebugSettings.DEBUG_INPUT) {
            debugInput.update()
        }
        if (!GameDebugSettings.DISABLE_HUD) {
            gameSessionData.gameSessionDataHud.stage.act(deltaTime)
            gameSessionData.gameSessionDataHud.stage.draw()
        }
    }

    companion object {
        const val JOYSTICK_PADDING = 64F
    }
}
