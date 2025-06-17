package com.gadarts.returnfire.systems.hud

import com.badlogic.ashley.core.Family
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.ai.BaseAiComponent
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.hud.HudSystem.Companion.JOYSTICK_PADDING
import com.gadarts.returnfire.systems.hud.osii.OnScreenInputInitializerApache
import com.gadarts.returnfire.systems.hud.osii.OnScreenInputInitializerTank
import com.gadarts.returnfire.systems.hud.react.HudSystemOnLandingIndicatorVisibilityChanged
import com.gadarts.returnfire.systems.hud.react.HudSystemOnPlayerAimSky
import com.gadarts.shared.model.definitions.CharacterDefinition
import com.gadarts.shared.model.definitions.SimpleCharacterDefinition
import com.gadarts.shared.model.definitions.TurretCharacterDefinition

class HudSystemImpl(gamePlayManagers: GamePlayManagers) : HudSystem,
    GameEntitySystem(gamePlayManagers),
    InputProcessor {
    private val ui: Table by lazy { addUiTable() }
    private val radar: Radar by lazy {
        Radar(
            gameSessionData.gamePlayData.player!!,
            engine.getEntitiesFor(Family.all(BaseAiComponent::class.java).get()),
            gamePlayManagers.assetsManager,
            gameSessionData.mapData.bitMap
        )
    }
    private val hudButtons = HudButtons(gamePlayManagers)
    private val onScreenInputInitializers: Map<CharacterDefinition, (Table, Cell<Touchpad>) -> Unit> by lazy {
        mapOf(
            SimpleCharacterDefinition.APACHE to
                    OnScreenInputInitializerApache(this, hudButtons, gameSessionData.autoAim),
            TurretCharacterDefinition.TANK to
                    OnScreenInputInitializerTank(
                        this,
                        hudButtons,
                        gameSessionData.hudData.turretTouchpad,
                    )
        )
    }
    private val debugInput: CameraInputController by lazy { CameraInputController(gameSessionData.renderData.camera) }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.LANDING_INDICATOR_VISIBILITY_CHANGED to HudSystemOnLandingIndicatorVisibilityChanged(hudButtons),
        SystemEvents.PLAYER_AIM_SKY to HudSystemOnPlayerAimSky(hudButtons),
        SystemEvents.PLAYER_ADDED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                if (gameSessionData.runsOnMobile) {
                    val hudTable = Table()
                    val topRowTable = Table()
                    hudTable.setDebug(GameDebugSettings.UI_DEBUG, true)
                    topRowTable.setDebug(GameDebugSettings.UI_DEBUG, true)
                    addBoardingButton(topRowTable)
                    ui.add(topRowTable).expandX().growX()
                    ui.row()
                    ui.add(hudTable).bottom().growX().expandY()
                    addOnScreenInput(gameSessionData, hudTable)
                } else {
                    ui.add(radar).size(RADAR_SIZE, RADAR_SIZE).expand().right().bottom().pad(40F)
                }
            }

        }
    )

    override fun initialize(gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        super.initialize(gameSessionData, gamePlayManagers)
        initializeInput()
        val console = gameSessionData.hudData.console
        val stage = gameSessionData.hudData.stage
        stage.addActor(console)
        val minimap = gameSessionData.hudData.minimap
        minimap.isVisible = false
        stage.addActor(minimap)
        val minimapSize = Gdx.graphics.width / 2F
        minimap.setSize(minimapSize, minimapSize)
        val halfSize = minimapSize / 2F
        minimap.setPosition(
            Gdx.graphics.width / 2F - halfSize,
            Gdx.graphics.height / 2F - halfSize
        )
        console.toFront()
    }

    override fun resume(delta: Long) {

    }

    override fun update(deltaTime: Float) {
        if (GameDebugSettings.DEBUG_INPUT) {
            debugInput.update()
        }

        gameSessionData.gamePlayData.player
        if (!GameDebugSettings.DISABLE_HUD) {
            gameSessionData.hudData.stage.act(deltaTime)
            gameSessionData.hudData.stage.draw()
        }
    }

    override fun keyDown(keycode: Int): Boolean {
        if (keycode == Input.Keys.TAB) {
            val minimap = gameSessionData.hudData.minimap
            minimap.isVisible = !minimap.isVisible
            return true
        }
        return false
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

    override fun touchCancelled(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return false
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        return false
    }

    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        return false
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return false
    }

    override fun dispose() {
        radar.dispose()
    }

    private fun addOnScreenInput(
        gameSessionData: GameSessionData,
        ui: Table
    ) {
        val movementPad =
            addTouchpad(ui, this.gameSessionData.hudData.movementTouchpad)
                .pad(0F, JOYSTICK_PADDING, 32F, 0F).bottom()
        val definition =
            ComponentsMapper.character.get(gameSessionData.gamePlayData.player).definition
        onScreenInputInitializers[definition]?.invoke(ui, movementPad)
    }


    override fun addButton(
        ui: Table,
        iconDefinition: String,
        clickListener: ClickListener,
        rightPadding: Float,
        visible: Boolean,
    ): Cell<ImageButton> {
        val up = TextureRegionDrawable(gamePlayManagers.assetsManager.getTexture("button_up"))
        val down =
            TextureRegionDrawable(gamePlayManagers.assetsManager.getTexture("button_down"))
        val icon =
            TextureRegionDrawable(gamePlayManagers.assetsManager.getTexture(iconDefinition))
        val button = ImageButton(ImageButton.ImageButtonStyle(up, down, null, icon, null, null))
        val cell = ui.add(button)
        if (rightPadding != 0F) {
            ui.pad(0F, 0F, 0F, rightPadding)
        }
        button.addListener(clickListener)
        button.isVisible = visible
        cell.size(BUTTON_SIZE)
        return cell
    }


    override fun addTouchpad(ui: Table, touchpad: Touchpad): Cell<Touchpad> {
        val joystickTexture = gamePlayManagers.assetsManager.getTexture("joystick")
        return ui.add(touchpad)
            .size(joystickTexture.width.toFloat(), joystickTexture.height.toFloat())
    }

    override fun addRadar(table: Table): Cell<Radar> {
        return table.add(radar).size(RADAR_SIZE, RADAR_SIZE)
    }

    private fun addBoardingButton(ui: Table) {
        val cell = addButton(
            ui,
            "icon_reverse",
            hudButtons.onBoardButtonClickListener,
            visible = false,
        )
        hudButtons.onboardButton = cell.actor
        cell.expandX().align(Align.right).padTop(BOARDING_BUTTON_PADDING)
            .padRight(BOARDING_BUTTON_PADDING)
    }


    private fun initializeInput() {
        if (GameDebugSettings.DEBUG_INPUT) {
            debugInput.autoUpdate = true
            Gdx.input.inputProcessor = debugInput
        } else {
            val inputMultiplexer = Gdx.input.inputProcessor as InputMultiplexer
            inputMultiplexer.addProcessor(gameSessionData.hudData.stage)
            inputMultiplexer.addProcessor(this)
        }
    }


    private fun addUiTable(): Table {
        val uiTable = Table()
        uiTable.debug(if (GameDebugSettings.UI_DEBUG) Table.Debug.all else Table.Debug.none)
        uiTable.name = GameSessionData.UI_TABLE_NAME
        uiTable.setFillParent(true)
        val stage = gameSessionData.hudData.stage
        uiTable.setSize(stage.width, stage.height)
        stage.addActor(uiTable)
        return uiTable
    }

    companion object {
        private const val BOARDING_BUTTON_PADDING = 25F
        private const val BUTTON_SIZE = 150F
        private const val RADAR_SIZE = 200F
    }
}
