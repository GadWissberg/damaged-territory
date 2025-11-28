@file:Suppress("RedundantSuppression")

package com.gadarts.returnfire.ecs.systems.ai

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.ai.BaseAiComponent
import com.gadarts.returnfire.ecs.components.ai.GroundCharacterAiComponent
import com.gadarts.returnfire.ecs.systems.GameEntitySystem
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.ai.logic.AiLogicHandler
import com.gadarts.returnfire.ecs.systems.ai.react.AiSystemOnOpponentCharacterCreated
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.ecs.systems.physics.BulletEngineHandler
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.characters.CharacterDefinition
import com.gadarts.shared.data.definitions.characters.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.characters.TurretCharacterDefinition


class AiSystemImpl(gamePlayManagers: GamePlayManagers) : AiSystem, GameEntitySystem(gamePlayManagers) {

    private val aiComponentInitializers = mapOf(
        SimpleCharacterDefinition.APACHE to { entity: Entity ->
            gamePlayManagers.ecs.entityBuilder.addApacheAiComponentToEntity(
                entity,
                ComponentsMapper.character.get(entity).definition.getHP()
            )
        },
        TurretCharacterDefinition.TANK to { entity: Entity ->
            initializeGroundCharacterAiComponents(entity, gamePlayManagers)
        }, TurretCharacterDefinition.JEEP to { entity: Entity ->
            initializeGroundCharacterAiComponents(entity, gamePlayManagers)
        }
    )

    private fun initializeGroundCharacterAiComponents(
        entity: Entity,
        gamePlayManagers: GamePlayManagers
    ): GroundCharacterAiComponent {
        val turretBaseComponent = ComponentsMapper.turretBase.get(entity)
        val turret = turretBaseComponent.turret
        if (turretBaseComponent != null && turret != null) {
            gamePlayManagers.ecs.entityBuilder.addAiTurretComponentToEntity(
                turret,
            )
        }
        return gamePlayManagers.ecs.entityBuilder.addGroundCharacterAiComponentToEntity(
            entity,
        )
    }

    private val logicHandler: AiLogicHandler by lazy {
        AiLogicHandler(
            gameSessionData, engine.getEntitiesFor(
                Family.all(BaseAiComponent::class.java)
                    .get()
            ), gamePlayManagers, autoAim,
            engine
        )
    }
    private val autoAim by lazy {
        gamePlayManagers.factories.autoAimShapeFactory.generate(
            BulletEngineHandler.COLLISION_GROUP_GREEN,
            BulletEngineHandler.COLLISION_GROUP_BROWN,
        )
    }


    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> by lazy {
        mapOf(
            SystemEvents.OPPONENT_CHARACTER_CREATED to AiSystemOnOpponentCharacterCreated(
                this
            ),
            SystemEvents.CHARACTER_ONBOARDING_FINISHED to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val character = msg.extraInfo as Entity
                    if (ComponentsMapper.baseAi.has(character)) {
                        logicHandler.onVehicleOnboarded(character)
                    }
                }
            },
            SystemEvents.ELEVATOR_EMPTY_ONBOARD to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val character = msg.extraInfo as Entity
                    if (ComponentsMapper.hangar.get(ComponentsMapper.elevator.get(character).hangar).color == CharacterColor.GREEN) {
                        logicHandler.onElevatorEmptyOnboard()
                    }
                }
            },
            SystemEvents.CHARACTER_DIED to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val character = msg.extraInfo as Entity
                    val characterComponent = ComponentsMapper.character.get(character)
                    if (characterComponent.color == CharacterColor.GREEN && characterComponent.definition.isDeployable()) {
                        logicHandler.onCharacterDied()
                    }
                }
            }
        )
    }

    override fun onSystemReady() {
        logicHandler.begin()
    }

    @Suppress("SimplifyBooleanWithConstants")
    override fun update(deltaTime: Float) {
        val player = gameSessionData.gamePlayData.player ?: return
        if (gamePlayManagers.assetsManager.gameSettings.aiDisabled
            || isGamePaused()
            || ComponentsMapper.boarding.get(player).isBoarding()
        ) return

        logicHandler.update(deltaTime)
    }


    override fun resume(delta: Long) {
    }

    override fun dispose() {
        autoAim.dispose()
        logicHandler.dispose()
    }

    override fun invokeAiComponentInitializer(definition: CharacterDefinition, character: Entity) {
        aiComponentInitializers[definition]?.invoke(character)
    }

    override fun getAiLogicHandler(): AiLogicHandler {
        return logicHandler
    }


}
