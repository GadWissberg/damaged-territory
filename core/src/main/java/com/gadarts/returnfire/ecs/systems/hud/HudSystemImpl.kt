package com.gadarts.returnfire.ecs.systems.hud

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.actions.DelayAction
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.FlagComponent
import com.gadarts.returnfire.ecs.components.ai.BaseAiComponent
import com.gadarts.returnfire.ecs.systems.GameEntitySystem
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionState
import com.gadarts.returnfire.ecs.systems.data.hud.Minimap
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.ecs.systems.hud.HudSystem.Companion.HUD_ELEMENT_PADDING_BOTTOM
import com.gadarts.returnfire.ecs.systems.hud.HudSystem.Companion.JOYSTICK_PADDING_HORIZONTAL
import com.gadarts.returnfire.ecs.systems.hud.osii.OnScreenInputInitializerApache
import com.gadarts.returnfire.ecs.systems.hud.osii.OnScreenInputInitializerGroundCharacter
import com.gadarts.returnfire.ecs.systems.hud.react.HudSystemOnLandingIndicatorVisibilityChanged
import com.gadarts.returnfire.ecs.systems.hud.react.HudSystemOnPlayerAimSky
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.screens.Screens
import com.gadarts.returnfire.screens.types.gameplay.ToHangarScreenSwitchParameters
import com.gadarts.shared.assets.definitions.FontDefinition
import com.gadarts.shared.assets.definitions.SoundDefinition
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.characters.CharacterDefinition
import com.gadarts.shared.data.definitions.characters.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.characters.TurretCharacterDefinition

class HudSystemImpl(gamePlayManagers: GamePlayManagers) : HudSystem,
    GameEntitySystem(gamePlayManagers) {

    private val minimap by lazy {
        Minimap(
            gameSessionData.mapData.loadedMap,
            gamePlayManagers.assetsManager,
            gameSessionData.gamePlayData,
            gamePlayManagers.ecs.engine.getEntitiesFor(Family.all(FlagComponent::class.java).get())
        )
    }
    private val hudInputProcessor by lazy {
        HudInputProcessor(
            minimap,
            gameSessionData.gamePlayData
        )
    }
    private val radar: com.gadarts.returnfire.ecs.systems.hud.radar.Radar by lazy {
        com.gadarts.returnfire.ecs.systems.hud.radar.Radar(
            gameSessionData.gamePlayData,
            engine.getEntitiesFor(Family.all(BaseAiComponent::class.java).get()),
            gamePlayManagers.assetsManager,
            gameSessionData.mapData.groundBitMap
        )
    }
    private val hudButtons = HudButtons(gamePlayManagers)
    private val onScreenInputInitializers: Map<CharacterDefinition, (Table, Cell<Touchpad>) -> Unit> by lazy {
        mapOf(
            SimpleCharacterDefinition.APACHE to
                OnScreenInputInitializerApache(this, hudButtons, gameSessionData.autoAim),
            TurretCharacterDefinition.TANK to
                OnScreenInputInitializerGroundCharacter(
                    this,
                    hudButtons,
                    gameSessionData.hudData.turretTouchpad,
                    false,
                    gamePlayManagers.assetsManager.gameSettings
                ),
            TurretCharacterDefinition.JEEP to
                OnScreenInputInitializerGroundCharacter(
                    this,
                    hudButtons,
                    null,
                    true,
                    gamePlayManagers.assetsManager.gameSettings
                )
        )
    }
    private val debugInput: CameraInputController by lazy { CameraInputController(gameSessionData.renderData.camera) }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.LANDING_INDICATOR_VISIBILITY_CHANGED to HudSystemOnLandingIndicatorVisibilityChanged(
            hudButtons
        ),
        SystemEvents.PLAYER_AIM_SKY to HudSystemOnPlayerAimSky(hudButtons),
        SystemEvents.PLAYER_ADDED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                gameSessionData.hudData.ui = createUiTable()
                gameSessionData.hudData.movementTouchpad = createTouchpad()
                gameSessionData.hudData.turretTouchpad = createTouchpad()
                if (gameSessionData.runsOnMobile) {
                    val table = Table()
                    table.setSize(
                        Gdx.graphics.backBufferWidth.toFloat(),
                        Gdx.graphics.backBufferHeight.toFloat()
                    )
                    table.setFillParent(true)
                    table.setDebug(gamePlayManagers.assetsManager.gameSettings.uiDebug, true)
                    gameSessionData.hudData.ui.add(table).bottom().growX().expandY()
                    addBoardingButton(table)
                    table.row()
                    addOnScreenInput(gameSessionData, table)
                } else {
                    gameSessionData.hudData.ui.add(radar).size(RADAR_SIZE, RADAR_SIZE).expand()
                        .right().bottom().pad(40F)
                }
                gameSessionData.hudData.stage.addActor(gameSessionData.hudData.ui)
            }
        },
        SystemEvents.CHARACTER_DIED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                val entity = msg.extraInfo as? Entity ?: return
                if (entity == gameSessionData.gamePlayData.player) {
                    fadeToVehicleSelection(disposeScreen = false, delay = true)
                }
            }
        },
        SystemEvents.FLAG_POINT to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                if (gameSessionData.gamePlayData.gameSessionState == GameSessionState.GAME_OVER) return

                val color = msg.extraInfo as CharacterColor
                gameOver(color)
            }
        },
        SystemEvents.CHARACTER_ONBOARDING_FINISHED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                if (!ComponentsMapper.player.has(msg.extraInfo as Entity)) return

                fadeToVehicleSelection(disposeScreen = false, delay = false)
            }
        },
        SystemEvents.REMOVE_ENTITY to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                val entity = msg.extraInfo as? Entity ?: return
                if (!ComponentsMapper.player.has(entity)) return

                val hudData = gameSessionData.hudData
                hudData.ui.remove()
            }
        }
    )

    private fun createTouchpad(): Touchpad {
        val assetsManager = gamePlayManagers.assetsManager
        return Touchpad(
            15F,
            Touchpad.TouchpadStyle(
                TextureRegionDrawable(
                    assetsManager.getTexture(assetsManager.getTexturesDefinitions().definitions["joystick"]!!)
                ),
                TextureRegionDrawable(
                    assetsManager.getTexture(assetsManager.getTexturesDefinitions().definitions["joystick_center"]!!),
                )
            )
        )
    }

    private fun gameOver(
        winner: CharacterColor,
    ) {
        gamePlayManagers.soundManager.play(if (winner == CharacterColor.BROWN) SoundDefinition.GAME_WIN else SoundDefinition.GAME_OVER)
        gameSessionData.gamePlayData.gameSessionState = GameSessionState.GAME_OVER
        val label = Label(
            "${winner.name.uppercase()} WINS!",
            Label.LabelStyle(
                gamePlayManagers.assetsManager.getAssetByDefinition(FontDefinition.WOK_STENCIL_256),
                winner.color
            )
        )
        gameSessionData.hudData.stage.addActor(label)
        label.setPosition(
            Gdx.graphics.width / 2F - label.width / 2F,
            Gdx.graphics.height / 2F - label.height / 2F
        )
        fadeToVehicleSelection(disposeScreen = true, delay = true)
    }

    private fun fadeToVehicleSelection(
        disposeScreen: Boolean,
        delay: Boolean
    ) {
        gameSessionData.hudData.stage.addAction(
            Actions.sequence(
                DelayAction(if (delay) 5F else 0F),
                Actions.run {
                    gamePlayManagers.screensManager.setScreenWithFade(
                        Screens.VEHICLE_SELECTION,
                        2F,
                        ToHangarScreenSwitchParameters(disposeScreen)
                    )
                }
            )
        )
    }

    override fun initialize(gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        super.initialize(gameSessionData, gamePlayManagers)
        initializeInput()
        val console = gameSessionData.hudData.console
        val stage = gameSessionData.hudData.stage
        stage.addActor(console)
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
        if (gamePlayManagers.assetsManager.gameSettings.debugInput) {
            debugInput.update()
        }

        gameSessionData.gamePlayData.player
        if (!gamePlayManagers.assetsManager.gameSettings.disableHud) {
            gameSessionData.hudData.stage.act(deltaTime)
            gameSessionData.hudData.stage.draw()
        }

        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE) || Gdx.input.isKeyPressed(Input.Keys.BACK)) {
            fadeToVehicleSelection(disposeScreen = true, delay = false)
        }
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
                .pad(0F, JOYSTICK_PADDING_HORIZONTAL, HUD_ELEMENT_PADDING_BOTTOM, 0F).bottom()
                .left()
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
            .size(joystickTexture.width.toFloat(), joystickTexture.height.toFloat()).fill().bottom()
    }

    override fun addRadar(table: Table): Cell<com.gadarts.returnfire.ecs.systems.hud.radar.Radar> {
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
        if (gamePlayManagers.assetsManager.gameSettings.debugInput) {
            debugInput.autoUpdate = true
            Gdx.input.inputProcessor = debugInput
        } else {
            val inputMultiplexer = Gdx.input.inputProcessor as InputMultiplexer
            inputMultiplexer.addProcessor(gameSessionData.hudData.stage)
            inputMultiplexer.addProcessor(hudInputProcessor)
        }
    }

    private fun createUiTable(): Table {
        val uiTable = Table()
        uiTable.debug(if (gamePlayManagers.assetsManager.gameSettings.uiDebug) Table.Debug.all else Table.Debug.none)
        uiTable.name = GameSessionData.UI_TABLE_NAME
        uiTable.setFillParent(true)
        val stage = gameSessionData.hudData.stage
        uiTable.setSize(stage.width, stage.height)
        return uiTable
    }

    companion object {
        private const val BOARDING_BUTTON_PADDING = 25F
        private const val BUTTON_SIZE = 150F
        private const val RADAR_SIZE = 200F
    }
}
