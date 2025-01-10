package com.gadarts.returnfire.systems.hud

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Cell
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Align
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.model.TurretCharacterDefinition
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class HudSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {
    private var onboardButton: ImageButton? = null
    private var manualAimButton: ImageButton? = null
    private val debugInput: CameraInputController by lazy { CameraInputController(gameSessionData.renderData.camera) }


    private val priWeaponButtonClickListener = object : ClickListener() {
        override fun touchDown(
            event: InputEvent,
            x: Float,
            y: Float,
            pointer: Int,
            button: Int
        ): Boolean {
            this@HudSystem.gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_WEAPON_PRIMARY_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
            this@HudSystem.gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_WEAPON_PRIMARY_RELEASED.ordinal)
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
            this@HudSystem.gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_WEAPON_SECONDARY_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
            this@HudSystem.gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_WEAPON_SECONDARY_RELEASED.ordinal)
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
            this@HudSystem.gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_REVERSE_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }

        override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
            this@HudSystem.gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_REVERSE_RELEASED.ordinal)
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
            this@HudSystem.gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_ONBOARD_PRESSED.ordinal)
            if (onboardButton != null) {
                onboardButton!!.isVisible = false
            }
            return super.touchDown(event, x, y, pointer, button)
        }
    }
    private val manualAimButtonClickListener = object : ClickListener() {
        override fun touchDown(
            event: InputEvent,
            x: Float,
            y: Float,
            pointer: Int,
            button: Int
        ): Boolean {
            this@HudSystem.gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.BUTTON_MANUAL_AIM_PRESSED.ordinal)
            return super.touchDown(event, x, y, pointer, button)
        }
    }

    override fun initialize(gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        super.initialize(gameSessionData, gamePlayManagers)
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
            val definition =
                ComponentsMapper.character.get(gameSessionData.gamePlayData.player).definition
            if (definition == SimpleCharacterDefinition.APACHE) {
                movementPad.growX()
                addApacheButtons(ui)
            } else if (definition == TurretCharacterDefinition.TANK) {
                val touchpad = this.gameSessionData.hudData.turretTouchpad
                val imageButtonCell = addButton(
                    ui,
                    "icon_reverse",
                    reverseButtonClickListener
                )
                imageButtonCell.growX().left().bottom().padBottom(JOYSTICK_PADDING)
                addButton(
                    ui,
                    "icon_missiles",
                    secWeaponButtonClickListener,
                    JOYSTICK_PADDING,
                )
                addTouchpad(ui, touchpad).padRight(JOYSTICK_PADDING).padBottom(JOYSTICK_PADDING)
                    .right()
            }
        }
    }

    private fun addManualAimButton(ui: Table) {
        if (gameSessionData.autoAim) return

        val cell = addButton(
            ui,
            "icon_manual_aim_sky",
            manualAimButtonClickListener,
        ).size(150F)
        manualAimButton = cell.actor
        cell.right().top().padBottom(JOYSTICK_PADDING)
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
                if (onboardButton != null) {
                    onboardButton!!.isVisible = msg.extraInfo as Boolean
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
                    manualAimButton!!.image.drawable =
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
                val ui = addUiTable()
                val hudTable = Table()
                val topRowTable = Table()
                hudTable.setDebug(GameDebugSettings.UI_DEBUG, true)
                addBoardingButton(topRowTable)
                addManualAimButton(topRowTable)
                ui.add(topRowTable).expandX()
                ui.row()
                ui.add(hudTable)
                addOnScreenInput(gameSessionData, hudTable)
            }

        }
    )

    private fun addBoardingButton(ui: Table) {
        val cell = addButton(
            ui,
            "icon_reverse",
            onBoardButtonClickListener,
            visible = false
        )
        onboardButton = cell.actor
        cell.right().top().padBottom(JOYSTICK_PADDING)
    }

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
        val stage = gameSessionData.hudData.stage
        uiTable.setSize(stage.width, stage.height)
        uiTable.align(Align.bottom)
        stage.addActor(uiTable)
        return uiTable
    }

    override fun update(deltaTime: Float) {
        if (gameSessionData.gamePlayData.sessionFinished) return

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
        private const val BUTTON_SIZE = 150F
    }
}
