package com.gadarts.returnfire.systems.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Align
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Services
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
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    override fun resume(delta: Long) {

    }

    override fun dispose() {

    }


    private fun initializeInput() {
        if (GameDebugSettings.DEBUG_INPUT) {
            debugInput = CameraInputController(gameSessionData.camera)
            debugInput.autoUpdate = true
            Gdx.input.inputProcessor = debugInput
        } else {
            (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(gameSessionData.stage)
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

}
