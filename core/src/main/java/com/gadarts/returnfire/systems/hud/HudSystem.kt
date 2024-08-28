package com.gadarts.returnfire.systems.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Managers
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
            managers.dispatcher.dispatchMessage(SystemEvents.WEAPON_BUTTON_PRIMARY_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
            managers.dispatcher.dispatchMessage(SystemEvents.WEAPON_BUTTON_PRIMARY_RELEASED.ordinal)
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
            managers.dispatcher.dispatchMessage(SystemEvents.WEAPON_BUTTON_SECONDARY_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
            managers.dispatcher.dispatchMessage(SystemEvents.WEAPON_BUTTON_SECONDARY_RELEASED.ordinal)
            super.touchUp(event, x, y, pointer, button)
        }
    }

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        val ui = addUiTable()
        if (gameSessionData.runsOnMobile) {
            addJoystick(ui)
            addWeaponButton(
                ui,
                "icon_bullets",
                clickListener = priWeaponButtonClickListener
            )
            addWeaponButton(
                ui,
                "icon_missiles",
                JOYSTICK_PADDING_LEFT,
                secWeaponButtonClickListener
            )
        }
        initializeInput()
    }

    private fun addWeaponButton(
        ui: Table,
        iconDefinition: String,
        rightPadding: Float = 0F,
        clickListener: ClickListener
    ) {
        val up = TextureRegionDrawable(managers.assetsManager.getTexture("button_up"))
        val down =
            TextureRegionDrawable(managers.assetsManager.getTexture("button_down"))
        val icon =
            TextureRegionDrawable(managers.assetsManager.getTexture(iconDefinition))
        val button = ImageButton(ImageButton.ImageButtonStyle(up, down, null, icon, null, null))
        ui.add(button)
        if (rightPadding != 0F) {
            ui.pad(0F, 0F, 0F, rightPadding)
        }
        button.addListener(clickListener)
    }

    private fun addJoystick(ui: Table) {
        val joystickTexture = managers.assetsManager.getTexture("joystick")
        ui.add(gameSessionData.gameSessionDataHud.touchpad)
            .size(joystickTexture.width.toFloat(), joystickTexture.height.toFloat())
            .pad(0F, JOYSTICK_PADDING_LEFT, 64F, 0F)
            .growX()
            .left()
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
        super.update(deltaTime)
        if (GameDebugSettings.DEBUG_INPUT) {
            debugInput.update()
        }
        gameSessionData.gameSessionDataHud.stage.act(deltaTime)
        gameSessionData.gameSessionDataHud.stage.draw()
    }

    companion object {
        const val JOYSTICK_PADDING_LEFT = 64F
    }
}
