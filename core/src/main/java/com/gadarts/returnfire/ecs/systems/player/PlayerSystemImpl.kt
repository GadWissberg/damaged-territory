package com.gadarts.returnfire.ecs.systems.player

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.ElevatorComponent
import com.gadarts.returnfire.ecs.components.cd.ChildDecalComponent
import com.gadarts.returnfire.ecs.systems.GameEntitySystem
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.ecs.systems.events.SystemEvents.*
import com.gadarts.returnfire.ecs.systems.events.data.RemoveComponentEventData
import com.gadarts.returnfire.ecs.systems.physics.BulletEngineHandler
import com.gadarts.returnfire.ecs.systems.player.handlers.PlayerShootingHandler
import com.gadarts.returnfire.ecs.systems.player.handlers.movement.GroundVehicleMovementHandlerMobile
import com.gadarts.returnfire.ecs.systems.player.handlers.movement.VehicleMovementHandler
import com.gadarts.returnfire.ecs.systems.player.handlers.movement.apache.ApacheMovementHandlerDesktop
import com.gadarts.returnfire.ecs.systems.player.handlers.movement.apache.ApacheMovementHandlerMobile
import com.gadarts.returnfire.ecs.systems.player.handlers.movement.jeep.JeepMovementHandlerDesktop
import com.gadarts.returnfire.ecs.systems.player.handlers.movement.jeep.JeepMovementHandlerParams
import com.gadarts.returnfire.ecs.systems.player.handlers.movement.tank.TankMovementHandlerDesktop
import com.gadarts.returnfire.ecs.systems.player.handlers.movement.tank.TankMovementHandlerParams
import com.gadarts.returnfire.ecs.systems.player.handlers.movement.touchpad.MovementTouchPadListener
import com.gadarts.returnfire.ecs.systems.player.handlers.movement.touchpad.TurretTouchPadListener
import com.gadarts.returnfire.ecs.systems.player.react.*
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.characters.CharacterDefinition
import com.gadarts.shared.data.definitions.characters.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.characters.TurretCharacterDefinition

class PlayerSystemImpl(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers),
    PlayerSystem,
    InputProcessor {
    private val playerMovementHandlersDesktop = mapOf<CharacterDefinition, VehicleMovementHandler>(
        SimpleCharacterDefinition.APACHE to ApacheMovementHandlerDesktop(),
        TurretCharacterDefinition.TANK to TankMovementHandlerDesktop(),
        TurretCharacterDefinition.JEEP to JeepMovementHandlerDesktop()
    )

    private val playerMovementHandlersMobile: Map<CharacterDefinition, VehicleMovementHandler> =
        mapOf(
            SimpleCharacterDefinition.APACHE to ApacheMovementHandlerMobile(),
            TurretCharacterDefinition.TANK to GroundVehicleMovementHandlerMobile(
                TankMovementHandlerParams()
            ),
            TurretCharacterDefinition.JEEP to GroundVehicleMovementHandlerMobile(
                JeepMovementHandlerParams()
            )
        )

    private val playerStage: Entity by lazy {
        engine.getEntitiesFor(
            Family.all(ElevatorComponent::class.java).get()
        )
            .find { ComponentsMapper.hangar.get(ComponentsMapper.elevator.get(it).hangar).color == CharacterColor.BROWN }!!
    }
    private val autoAim by lazy {
        if (gameSessionData.autoAim) {
            gamePlayManagers.factories.autoAimShapeFactory.generate(
                BulletEngineHandler.COLLISION_GROUP_PLAYER,
                BulletEngineHandler.COLLISION_GROUP_AI
            )
        } else {
            null
        }
    }
    private val playerShootingHandler =
        PlayerShootingHandler(
            gamePlayManagers.soundManager,
            gamePlayManagers.assetsManager
        )

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        BUTTON_WEAPON_PRIMARY_PRESSED to PlayerSystemOnButtonWeaponPrimaryPressed(
            playerShootingHandler,
        ),
        BUTTON_WEAPON_PRIMARY_RELEASED to PlayerSystemOnButtonWeaponPrimaryReleased(
            playerShootingHandler
        ),
        BUTTON_WEAPON_SECONDARY_PRESSED to PlayerSystemOnButtonWeaponSecondaryPressed(
            playerShootingHandler,
        ),
        BUTTON_WEAPON_SECONDARY_RELEASED to PlayerSystemOnButtonWeaponSecondaryReleased(
            playerShootingHandler
        ),
        BUTTON_REVERSE_PRESSED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                val player = gameSessionData.gamePlayData.player ?: return

                gameSessionData.gamePlayData.playerMovementHandler.onReverseScreenButtonPressed(
                    player
                )
            }
        },
        BUTTON_REVERSE_RELEASED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                val player = gameSessionData.gamePlayData.player ?: return

                gameSessionData.gamePlayData.playerMovementHandler.onReverseScreenButtonReleased(
                    player
                )
            }
        },
        CHARACTER_DEPLOYMENT_DONE to PlayerSystemOnCharacterDeployed(this),
        BUTTON_ONBOARD_PRESSED to PlayerSystemOnButtonOnboardPressed(),
        OPPONENT_CHARACTER_CREATED to object : HandlerOnEvent {
            @Suppress("SimplifyBooleanWithConstants", "RedundantSuppression")
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                val entity = msg.extraInfo as Entity
                if (ComponentsMapper.character.get(entity).color == CharacterColor.BROWN) {
                    val oldPlayer = gameSessionData.gamePlayData.player
                    if (oldPlayer != null) {
                        RemoveComponentEventData.set(
                            oldPlayer,
                            ComponentsMapper.player.get(gameSessionData.gamePlayData.player)
                        )
                        gamePlayManagers.dispatcher.dispatchMessage(REMOVE_COMPONENT.ordinal)
                    }
                    gameSessionData.gamePlayData.player = entity
                    if (gamePlayManagers.assetsManager.gameSettings.forcePlayerHp >= 0) {
                        ComponentsMapper.character.get(entity).hp =
                            gamePlayManagers.assetsManager.gameSettings.forcePlayerHp
                    }
                    val modelInstanceComponent = ComponentsMapper.modelInstance.get(entity)
                    modelInstanceComponent.hidden = gamePlayManagers.assetsManager.gameSettings.hidePlayer
                    gamePlayManagers.ecs.entityBuilder.addPlayerComponentToEntity(entity)
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
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                if (ComponentsMapper.player.has(msg.extraInfo as Entity)) {
                    gamePlayManagers.dispatcher.dispatchMessage(
                        REMOVE_ENTITY.ordinal,
                        gameSessionData.gamePlayData.player
                    )
                    playerRemoved()
                }
            }
        },
        REMOVE_ENTITY to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                val entity = msg.extraInfo as Entity
                if (!ComponentsMapper.player.has(entity)) return

                playerRemoved()
            }
        },
    )

    override fun resume(delta: Long) {

    }

    override fun dispose() {
        autoAim?.dispose()
    }

    override fun update(deltaTime: Float) {
        if (shouldSkipUpdate()) return

        val player = gameSessionData.gamePlayData.player
        gameSessionData.gamePlayData.playerMovementHandler.update(
            player!!,
            deltaTime
        )
        val turretBaseComponent = ComponentsMapper.turretBase.get(player)
        if (turretBaseComponent == null || !ComponentsMapper.turretAutomation.has(
                turretBaseComponent.turret
            )
        ) {
            playerShootingHandler.update(player)
        }
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
        return (isGamePaused()
            || player == null
            || (!ComponentsMapper.boarding.has(player) || ComponentsMapper.boarding.get(player)
            .isBoarding())
            || (!ComponentsMapper.character.has(player) || ComponentsMapper.character.get(player).dead))
    }

    override fun keyDown(keycode: Int): Boolean {
        val player = gameSessionData.gamePlayData.player
        if (player == null || ComponentsMapper.boarding.get(player).isBoarding()) return false

        when (keycode) {
            Input.Keys.UP -> {
                gameSessionData.gamePlayData.playerMovementHandler.thrust(player)
            }

            Input.Keys.DOWN -> {
                gameSessionData.gamePlayData.playerMovementHandler.reverse()
            }

            Input.Keys.LEFT -> {
                gameSessionData.gamePlayData.playerMovementHandler.pressedLeft(player)
            }

            Input.Keys.RIGHT -> {
                gameSessionData.gamePlayData.playerMovementHandler.pressedRight(player)
            }

            Input.Keys.CONTROL_LEFT -> {
                playerShootingHandler.startPrimaryShooting(player)
            }

            Input.Keys.SHIFT_LEFT -> {
                if (ComponentsMapper.childDecal.get(playerStage).visible) {
                    gamePlayManagers.dispatcher.dispatchMessage(
                        CHARACTER_REQUEST_BOARDING.ordinal,
                        player
                    )
                } else {
                    playerShootingHandler.startSecondaryShooting(player)
                }
            }

            Input.Keys.ENTER -> {
                if (!gameSessionData.autoAim && ComponentsMapper.character.get(
                        player
                    ).definition.isFlyer()
                ) {
                    playerShootingHandler.toggleSkyAim()
                }
            }

            Input.Keys.ALT_LEFT -> {
                gameSessionData.gamePlayData.playerMovementHandler.pressedAlt(player)
            }

        }
        return false
    }

    override fun keyUp(keycode: Int): Boolean {
        val player = gameSessionData.gamePlayData.player
        if (player == null || ComponentsMapper.boarding.get(player).isBoarding()) return false

        when (keycode) {
            Input.Keys.UP, Input.Keys.DOWN, Input.Keys.LEFT, Input.Keys.RIGHT -> {
                gameSessionData.gamePlayData.playerMovementHandler.onMovementTouchUp(
                    player,
                    keycode,
                )
            }

            Input.Keys.CONTROL_LEFT -> {
                playerShootingHandler.stopPrimaryShooting()
            }

            Input.Keys.SHIFT_LEFT -> {
                playerShootingHandler.stopSecondaryShooting()
            }

            Input.Keys.ALT_LEFT -> {
                gameSessionData.gamePlayData.playerMovementHandler.releasedAlt(player)
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
        if (gameSessionData.gamePlayData.player == null) return

        if (gameSessionData.runsOnMobile) {
            gameSessionData.hudData.movementTouchpad.addListener(
                MovementTouchPadListener(
                    gameSessionData.gamePlayData
                )
            )
            gameSessionData.hudData.turretTouchpad.addListener(
                TurretTouchPadListener(
                    gameSessionData.gamePlayData,
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
        return if (runsOnMobile) {
            playerMovementHandlersMobile[characterDefinition]!!
        } else {
            playerMovementHandlersDesktop[characterDefinition]!!
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

    private fun playerRemoved() {
        gameSessionData.gamePlayData.player = null
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private const val LANDING_OK_OFFSET = 0.75F
    }
}
