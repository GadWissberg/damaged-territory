@file:Suppress("RedundantSuppression")

package com.gadarts.returnfire.ecs.systems.ai

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.MathUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.ai.BaseAiComponent
import com.gadarts.returnfire.ecs.components.ai.GroundCharacterAiComponent
import com.gadarts.returnfire.ecs.systems.GameEntitySystem
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.ai.logic.AiCharacterLogicHandler
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.ecs.systems.events.data.OpponentEnteredGameplayScreenEventData
import com.gadarts.returnfire.ecs.systems.physics.BulletEngineHandler
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.TurretCharacterDefinition


class AiSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {

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
        if (turretBaseComponent != null) {
            gamePlayManagers.ecs.entityBuilder.addAiTurretComponentToEntity(
                turretBaseComponent.turret,
            )
        }
        return gamePlayManagers.ecs.entityBuilder.addGroundCharacterAiComponentToEntity(
            entity,
        )
    }

    private val aiCharacterLogicHandler: AiCharacterLogicHandler by lazy {
        AiCharacterLogicHandler(
            gameSessionData, engine.getEntitiesFor(
                Family.all(BaseAiComponent::class.java)
                    .get()
            ), gamePlayManagers, autoAim,
            engine
        )
    }
    private val autoAim by lazy {
        gamePlayManagers.factories.autoAimShapeFactory.generate(
            BulletEngineHandler.COLLISION_GROUP_AI,
            BulletEngineHandler.COLLISION_GROUP_PLAYER,
        )
    }


    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.OPPONENT_CHARACTER_CREATED to object : HandlerOnEvent {
            @Suppress("SimplifyBooleanWithConstants", "KotlinConstantConditions")
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                val entity = msg.extraInfo as Entity
                val characterComponent = ComponentsMapper.character.get(entity)
                if (characterComponent.color == CharacterColor.GREEN) {
                    val definition = characterComponent.definition
                    gamePlayManagers.ecs.entityBuilder.addBaseAiComponentToEntity(
                        entity,
                        definition.getHP()
                    )
                    aiComponentInitializers[definition]?.invoke(entity)
                    if (GameDebugSettings.FORCE_ENEMY_HP >= 0) {
                        ComponentsMapper.character.get(entity).hp = GameDebugSettings.FORCE_ENEMY_HP
                    }
                    aiCharacterLogicHandler.onCharacterCreated(entity)
                }
            }
        },
    )

    override fun onSystemReady() {
        OpponentEnteredGameplayScreenEventData.set(
            CharacterColor.GREEN,
            if (MathUtils.randomBoolean()) SimpleCharacterDefinition.APACHE else TurretCharacterDefinition.TANK
        )
        gamePlayManagers.dispatcher.dispatchMessage(
            SystemEvents.OPPONENT_ENTERED_GAME_PLAY_SCREEN.ordinal,
        )
    }

    @Suppress("SimplifyBooleanWithConstants")
    override fun update(deltaTime: Float) {
        val player = gameSessionData.gamePlayData.player ?: return
        if (GameDebugSettings.AI_DISABLED
            || isGamePaused()
            || ComponentsMapper.boarding.get(player).isBoarding()
        ) return

        aiCharacterLogicHandler.update(deltaTime)
    }


    override fun resume(delta: Long) {
    }

    override fun dispose() {
        autoAim.dispose()
        aiCharacterLogicHandler.dispose()
    }




}
