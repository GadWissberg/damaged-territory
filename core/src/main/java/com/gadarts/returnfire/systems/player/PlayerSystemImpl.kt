package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.StageComponent
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.SystemEvents.*
import com.gadarts.returnfire.systems.physics.BulletEngineHandler
import com.gadarts.returnfire.systems.player.handlers.PlayerShootingHandler
import com.gadarts.returnfire.systems.player.handlers.movement.VehicleMovementHandler
import com.gadarts.returnfire.systems.player.handlers.movement.apache.ApacheMovementHandlerDesktop
import com.gadarts.returnfire.systems.player.handlers.movement.apache.ApacheMovementHandlerMobile
import com.gadarts.returnfire.systems.player.handlers.movement.tank.TankMovementHandlerDesktop
import com.gadarts.returnfire.systems.player.handlers.movement.tank.TankMovementHandlerMobile
import com.gadarts.returnfire.systems.player.handlers.movement.touchpad.MovementTouchPadListener
import com.gadarts.returnfire.systems.player.handlers.movement.touchpad.TurretTouchPadListener
import com.gadarts.returnfire.systems.player.react.*

/**
 * Responsible to initialize input, create the player and updates the player's character according to the input.
 */
class PlayerSystemImpl(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers), PlayerSystem,
    InputProcessor {

    private val playerStage: Entity by lazy {
        engine.getEntitiesFor(
            Family.all(StageComponent::class.java).get()
        ).find { ComponentsMapper.base.get(ComponentsMapper.stage.get(it).base).color == CharacterColor.BROWN }!!
    }
    private val autoAim by lazy {
        gamePlayManagers.factories.autoAimShapeFactory.generate(
            BulletEngineHandler.COLLISION_GROUP_PLAYER,
            BulletEngineHandler.COLLISION_GROUP_ENEMY
        )
    }
    private val playerShootingHandler = PlayerShootingHandler(gamePlayManagers.entityBuilder)

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        BUTTON_WEAPON_PRIMARY_PRESSED to PlayerSystemOnButtonWeaponPrimaryPressed(playerShootingHandler),
        BUTTON_WEAPON_PRIMARY_RELEASED to PlayerSystemOnButtonWeaponPrimaryReleased(playerShootingHandler),
        BUTTON_WEAPON_SECONDARY_PRESSED to PlayerSystemOnButtonWeaponSecondaryPressed(playerShootingHandler),
        BUTTON_WEAPON_SECONDARY_RELEASED to PlayerSystemOnButtonWeaponSecondaryReleased(playerShootingHandler),
        BUTTON_REVERSE_PRESSED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                gameSessionData.gamePlayData.playerMovementHandler.onReverseScreenButtonPressed()
            }
        },
        BUTTON_REVERSE_RELEASED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                gameSessionData.gamePlayData.playerMovementHandler.onReverseScreenButtonReleased()
            }
        },
        CHARACTER_OFF_BOARDED to PlayerSystemOnCharacterOffBoarded(this),
        CHARACTER_DIED to PlayerSystemOnCharacterDied(),
        BUTTON_ONBOARD_PRESSED to PlayerSystemOnButtonOnboardPressed(),
        OPPONENT_CHARACTER_CREATED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                val entity = msg.extraInfo as Entity
                if (ComponentsMapper.character.get(entity).color == CharacterColor.BROWN) {
                    gameSessionData.gamePlayData.player = entity
                    @Suppress("KotlinConstantConditions")
                    if (GameDebugSettings.FORCE_PLAYER_HP >= 0) {
                        ComponentsMapper.character.get(entity).hp = GameDebugSettings.FORCE_PLAYER_HP
                    }
                    val modelInstanceComponent = ComponentsMapper.modelInstance.get(entity)
                    modelInstanceComponent.hidden = GameDebugSettings.HIDE_PLAYER
                    gamePlayManagers.entityBuilder.addPlayerComponentToEntity(entity)
                    initializePlayerHandlers()
                    gamePlayManagers.dispatcher.dispatchMessage(PLAYER_ADDED.ordinal)
                }
            }
        },
        BUTTON_MANUAL_AIM_PRESSED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                playerShootingHandler.toggleSkyAim()
            }
        },
        CHARACTER_ONBOARDING_FINISHED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                if (ComponentsMapper.player.has(msg.extraInfo as Entity)) {
                    gamePlayManagers.screensManager.goToHangarScreen()
                }
            }
        }
    )

    override fun resume(delta: Long) {

    }

    override fun dispose() {
        autoAim.dispose()
    }

    override fun update(deltaTime: Float) {
        if (shouldSkipUpdate()) return

        val player = gameSessionData.gamePlayData.player!!
        gameSessionData.gamePlayData.playerMovementHandler.update(
            player,
            deltaTime
        )
        playerShootingHandler.update(player)
        val position =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val stagePosition =
            ComponentsMapper.modelInstance.get(playerStage).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector2
            )
        val childDecalComponent = ComponentsMapper.childDecal.get(playerStage)
        handleLandingIndicatorVisibility(childDecalComponent, position, stagePosition)
    }

    private fun shouldSkipUpdate(): Boolean {
        val player = gameSessionData.gamePlayData.player
        return (player == null
                || (!ComponentsMapper.boarding.has(player) || ComponentsMapper.boarding.get(player).isBoarding())
                || (!ComponentsMapper.character.has(player) || ComponentsMapper.character.get(player).dead))
    }

    override fun keyDown(keycode: Int): Boolean {
        if (gameSessionData.gamePlayData.player == null || ComponentsMapper.boarding.get(gameSessionData.gamePlayData.player)
                .isBoarding()
        ) return false

        when (keycode) {
            Input.Keys.UP -> {
                gameSessionData.gamePlayData.playerMovementHandler.thrust(gameSessionData.gamePlayData.player!!)
            }

            Input.Keys.DOWN -> {
                gameSessionData.gamePlayData.playerMovementHandler.reverse()
            }

            Input.Keys.LEFT -> {
                gameSessionData.gamePlayData.playerMovementHandler.pressedLeft()
            }

            Input.Keys.RIGHT -> {
                gameSessionData.gamePlayData.playerMovementHandler.pressedRight()
            }

            Input.Keys.CONTROL_LEFT -> {
                playerShootingHandler.startPrimaryShooting()
            }

            Input.Keys.SHIFT_LEFT -> {
                if (ComponentsMapper.childDecal.get(playerStage).visible) {
                    gamePlayManagers.dispatcher.dispatchMessage(
                        CHARACTER_REQUEST_BOARDING.ordinal,
                        gameSessionData.gamePlayData.player
                    )
                } else {
                    playerShootingHandler.startSecondaryShooting()
                }
            }

            Input.Keys.ENTER -> {
                if (!gameSessionData.autoAim) {
                    playerShootingHandler.toggleSkyAim()
                }
            }

            Input.Keys.ALT_LEFT -> {
                gameSessionData.gamePlayData.playerMovementHandler.pressedAlt()
            }

        }
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        if (gameSessionData.gamePlayData.player == null || ComponentsMapper.boarding.get(gameSessionData.gamePlayData.player)
                .isBoarding()
        ) return false

        when (keycode) {
            Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT -> {
                gameSessionData.gamePlayData.playerMovementHandler.onMovementTouchUp(keycode)
            }

            Input.Keys.CONTROL_LEFT -> {
                playerShootingHandler.stopPrimaryShooting()
            }

            Input.Keys.SHIFT_LEFT -> {
                playerShootingHandler.stopSecondaryShooting()
            }

            Input.Keys.ALT_LEFT -> {
                gameSessionData.gamePlayData.playerMovementHandler.releasedAlt()
            }

        }
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

    override fun initInputMethod() {
        if (gameSessionData.runsOnMobile) {
            gameSessionData.hudData.movementTouchpad.addListener(
                MovementTouchPadListener(
                    gameSessionData.gamePlayData.playerMovementHandler,
                    gameSessionData.gamePlayData.player!!
                )
            )
            gameSessionData.hudData.turretTouchpad.addListener(
                TurretTouchPadListener(
                    gameSessionData.gamePlayData.playerMovementHandler,
                    playerShootingHandler
                )
            )
        } else {
            (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(this)
        }
    }

    private fun createPlayerMovementHandler(): VehicleMovementHandler {
        val player = gameSessionData.gamePlayData.player
        val characterDefinition = ComponentsMapper.character.get(player).definition
        val runsOnMobile = gameSessionData.runsOnMobile
        return if (characterDefinition == SimpleCharacterDefinition.APACHE) {
            if (runsOnMobile) {
                ApacheMovementHandlerMobile()
            } else {
                ApacheMovementHandlerDesktop()
            }
        } else {
            if (runsOnMobile) {
                TankMovementHandlerMobile(player!!)
            } else {
                TankMovementHandlerDesktop(player!!)
            }
        }
    }

    private fun initializePlayerHandlers() {
        gameSessionData.gamePlayData.playerMovementHandler = createPlayerMovementHandler()
        playerShootingHandler.initialize(
            gamePlayManagers.dispatcher,
            gameSessionData,
            if (gameSessionData.autoAim) autoAim else null
        )
    }

    private fun handleLandingIndicatorVisibility(
        childDecalComponent: ChildDecalComponent,
        position: Vector3,
        stagePosition: Vector3
    ) {
        val oldValue = childDecalComponent.visible
        val newValue = (position.x <= stagePosition.x + LANDING_OK_OFFSET
                && position.x >= stagePosition.x - LANDING_OK_OFFSET
                && position.z <= stagePosition.z + LANDING_OK_OFFSET
                && position.z >= stagePosition.z - LANDING_OK_OFFSET)
        childDecalComponent.visible = newValue
        if (oldValue != newValue) {
            gamePlayManagers.dispatcher.dispatchMessage(
                LANDING_INDICATOR_VISIBILITY_CHANGED.ordinal,
                newValue
            )
        }
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private const val LANDING_OK_OFFSET = 0.75F
    }
}
