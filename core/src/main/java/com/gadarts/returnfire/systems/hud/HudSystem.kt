package com.gadarts.returnfire.systems.hud

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Services
import com.gadarts.returnfire.assets.TexturesDefinitions
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.GameSessionData
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.SystemEvents

class HudSystem : GameEntitySystem() {
    private lateinit var debugInput: CameraInputController


    private val priWeaponButtonClickListener = object : ClickListener() {
        override fun touchDown(
            event: InputEvent,
            x: Float,
            y: Float,
            pointer: Int,
            button: Int
        ): Boolean {
            services.dispatcher.dispatchMessage(SystemEvents.WEAPON_BUTTON_PRIMARY_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
            services.dispatcher.dispatchMessage(SystemEvents.WEAPON_BUTTON_PRIMARY_RELEASED.ordinal)
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
            services.dispatcher.dispatchMessage(SystemEvents.WEAPON_BUTTON_SECONDARY_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
            services.dispatcher.dispatchMessage(SystemEvents.WEAPON_BUTTON_SECONDARY_RELEASED.ordinal)
            super.touchUp(event, x, y, pointer, button)
        }
    }

    override fun initialize(gameSessionData: GameSessionData, services: Services) {
        super.initialize(gameSessionData, services)
        initializeInput()
        addUiTable()
        val ui = (gameSessionData.stage.actors.first {
            val name = it.name
            name != null && name.equals(GameSessionData.UI_TABLE_NAME)
        }) as Table
        addJoystick(ui)
        addWeaponButton(ui, TexturesDefinitions.ICON_BULLETS, clickListener = priWeaponButtonClickListener)
        addWeaponButton(
            ui,
            TexturesDefinitions.ICON_MISSILES, JOYSTICK_PADDING_LEFT, secWeaponButtonClickListener
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    override fun resume(delta: Long) {

    }

    private fun addJoystick(ui: Table) {
        val joystickTexture = services.assetsManager.getAssetByDefinition(TexturesDefinitions.JOYSTICK)
        ui.add(gameSessionData.touchpad)
            .size(joystickTexture.width.toFloat(), joystickTexture.height.toFloat())
            .pad(0F, JOYSTICK_PADDING_LEFT, JOYSTICK_PADDING_BOTTOM, 0F)
            .growX()
            .left()
    }

    private fun addWeaponButton(
        ui: Table,
        iconDefinition: TexturesDefinitions,
        rightPadding: Float = 0F,
        clickListener: ClickListener
    ) {
        val up = TextureRegionDrawable(services.assetsManager.getAssetByDefinition(TexturesDefinitions.BUTTON_UP))
        val down = TextureRegionDrawable(services.assetsManager.getAssetByDefinition(TexturesDefinitions.BUTTON_DOWN))
        val icon = TextureRegionDrawable(services.assetsManager.getAssetByDefinition(iconDefinition))
        val button = ImageButton(ImageButton.ImageButtonStyle(up, down, null, icon, null, null))
        ui.add(button)
        if (rightPadding != 0F) {
            ui.pad(0F, 0F, 0F, rightPadding)
        }
        button.addListener(clickListener)
    }

    override fun dispose() {

    }


    private fun initializeInput() {
        if (GameDebugSettings.DEBUG_INPUT) {
            debugInput = CameraInputController(gameSessionData.camera)
            debugInput.autoUpdate = true
            Gdx.input.inputProcessor = debugInput
        } else {
            Gdx.input.inputProcessor = gameSessionData.stage
        }
    }

    private fun addUiTable() {
        val uiTable = Table()
        uiTable.debug(if (GameDebugSettings.UI_DEBUG) Table.Debug.all else Table.Debug.none)
        uiTable.name = GameSessionData.UI_TABLE_NAME
        uiTable.setFillParent(true)
        uiTable.setSize(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        uiTable.align(Align.bottom)
        gameSessionData.stage.addActor(uiTable)
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        if (GameDebugSettings.DEBUG_INPUT) {
            debugInput.update()
        }
        gameSessionData.stage.act(deltaTime)
        gameSessionData.stage.draw()
    }

    companion object {
        const val JOYSTICK_PADDING_LEFT = 64F
        const val JOYSTICK_PADDING_BOTTOM = 64F
    }
}
