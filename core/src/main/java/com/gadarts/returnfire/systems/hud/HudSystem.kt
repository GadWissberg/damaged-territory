package com.gadarts.returnfire.systems.hud

import com.badlogic.ashley.core.Family
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.ai.BaseAiComponent
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.definitions.CharacterDefinition
import com.gadarts.returnfire.model.definitions.SimpleCharacterDefinition
import com.gadarts.returnfire.model.definitions.TurretCharacterDefinition
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class HudSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers), InputProcessor {
    private val ui: Table by lazy { addUiTable() }
    private val radar: Radar by lazy {
        Radar(
            gameSessionData.mapData.currentMap.tilesTexturesMap,
            gameSessionData.gamePlayData.player!!,
            engine.getEntitiesFor(Family.all(BaseAiComponent::class.java).get()),
            gamePlayManagers.assetsManager,
            gameSessionData.mapData.bitMap
        )
    }

    private val onScreenInputInitializers: Map<CharacterDefinition, (Table, Cell<Touchpad>) -> Unit> =
        mapOf(
            SimpleCharacterDefinition.APACHE to
                    { ui: Table, movementPad: Cell<Touchpad> ->
                        movementPad.expandX().left()
                        addApacheButtons(ui)
                    },
            TurretCharacterDefinition.TANK to
                    { ui: Table, _: Cell<Touchpad> ->
                        val touchpad = this.gameSessionData.hudData.turretTouchpad
                        val imageButtonCell = addButton(
                            ui,
                            "icon_reverse",
                            hudButtons.reverseButtonClickListener
                        )
                        imageButtonCell.grow().left().bottom().padBottom(32F)
                        val attackButtonsTable = Table()
                        attackButtonsTable.setDebug(GameDebugSettings.UI_DEBUG, true)
                        addButton(
                            attackButtonsTable,
                            "icon_missiles",
                            hudButtons.secWeaponButtonClickListener,
                        ).center().row()
                        ui.add(attackButtonsTable).right()
                        addTouchpad(ui, touchpad).pad(0F, 0F, 0F, JOYSTICK_PADDING).top()
                    }
        )
    private val hudButtons = HudButtons(gamePlayManagers)
    private val debugInput: CameraInputController by lazy { CameraInputController(gameSessionData.renderData.camera) }

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

    private fun addManualAimButton(ui: Table) {
        if (gameSessionData.autoAim) return

        val cell = addButton(
            ui,
            "icon_manual_aim_sky",
            hudButtons.manualAimButtonClickListener,
        ).size(MANUAL_AIM_BUTTON_SIZE)
        hudButtons.manualAimButton = cell.actor
        cell.right().top()
    }

    private fun addApacheButtons(ui: Table) {
        addButton(
            ui,
            "icon_bullets",
            clickListener = hudButtons.priWeaponButtonClickListener
        )
        addButton(
            ui,
            "icon_missiles",
            hudButtons.secWeaponButtonClickListener,
            JOYSTICK_PADDING,
        )
        addManualAimButton(ui)
    }

    private fun addButton(
        ui: Table,
        iconDefinition: String,
        clickListener: ClickListener,
        rightPadding: Float = 0F,
        visible: Boolean = true,
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

    private fun addTouchpad(ui: Table, touchpad: Touchpad): Cell<Touchpad> {
        val joystickTexture = gamePlayManagers.assetsManager.getTexture("joystick")
        return ui.add(touchpad)
            .size(joystickTexture.width.toFloat(), joystickTexture.height.toFloat())
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.LANDING_INDICATOR_VISIBILITY_CHANGED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                val onboardButton = hudButtons.onboardButton
                if (onboardButton != null) {
                    onboardButton.isVisible = msg.extraInfo as Boolean
                }
            }
        },
        SystemEvents.PLAYER_AIM_SKY to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                val name =
                    if (msg.extraInfo as Boolean) "icon_manual_aim_sky" else "icon_manual_aim_ground"
                if (!gameSessionData.runsOnMobile) {
                    val texture =
                        gamePlayManagers.assetsManager.getTexture(name)
                    val icon = Image(texture)
                    icon.setPosition(
                        Gdx.graphics.width / 2F - texture.width / 2F,
                        Gdx.graphics.height / 2F - texture.height / 2F
                    )
                    gameSessionData.hudData.stage.addActor(icon)
                    icon.addAction(
                        Actions.sequence(
                            Actions.fadeOut(1F, Interpolation.fade),
                            Actions.run { icon.remove() })
                    )
                } else {
                    hudButtons.manualAimButton!!.image.drawable =
                        TextureRegionDrawable(gamePlayManagers.assetsManager.getTexture(name))
                }
            }
        },
        SystemEvents.PLAYER_ADDED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                ui.add(radar).size(RADAR_SIZE, RADAR_SIZE).expand().right().bottom().pad(40F)
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
                }
            }

        }
    )

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

    override fun resume(delta: Long) {

    }

    override fun dispose() {
        radar.dispose()
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


    companion object {
        private const val JOYSTICK_PADDING = 64F
        private const val BOARDING_BUTTON_PADDING = 25F
        private const val BUTTON_SIZE = 150F
        private const val MANUAL_AIM_BUTTON_SIZE = 150F
        const val RADAR_SIZE = 192F
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
}
