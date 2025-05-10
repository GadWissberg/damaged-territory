@file:Suppress("RedundantSuppression")

package com.gadarts.returnfire.systems.ai

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.ai.BaseAiComponent
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.definitions.SimpleCharacterDefinition
import com.gadarts.returnfire.model.definitions.TurretCharacterDefinition
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.ai.logic.AiLogicHandler
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.physics.BulletEngineHandler


class AiSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {
    private val aiComponentInitializers = mapOf(
        SimpleCharacterDefinition.APACHE to { entity: Entity ->
            gamePlayManagers.ecs.entityBuilder.addApacheAiComponentToEntity(
                entity,
                ComponentsMapper.character.get(entity).definition.getHP()
            )
        },
        TurretCharacterDefinition.TANK to { entity: Entity ->
            val turretBaseComponent = ComponentsMapper.turretBase.get(entity)
            if (turretBaseComponent != null) {
                gamePlayManagers.ecs.entityBuilder.addAiTurretComponentToEntity(
                    turretBaseComponent.turret,
                )
            }
            gamePlayManagers.ecs.entityBuilder.addTankAiComponentToEntity(
                entity,
            )
        }
    )
    private val aiLogicHandler: AiLogicHandler by lazy {
        AiLogicHandler(
            gameSessionData, gamePlayManagers, autoAim, engine.getEntitiesFor(
                Family.all(BaseAiComponent::class.java)
                    .get()
            ),
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
                    if (gameSessionData.gamePlayData.player != null) {
                        setTargetForAi(entity, gameSessionData.gamePlayData.player!!)
                    }
                } else {
                    aiEntities.forEach {
                        setTargetForAi(it, entity)
                    }
                }
            }
        },
    )

    @Suppress("SimplifyBooleanWithConstants")
    override fun update(deltaTime: Float) {
        if (GameDebugSettings.AI_DISABLED
            || gameSessionData.hudData.console.isActive()
            || gameSessionData.gamePlayData.player == null
            || ComponentsMapper.boarding.get(gameSessionData.gamePlayData.player).isBoarding()
        ) return

        aiLogicHandler.update(deltaTime)
    }


    override fun resume(delta: Long) {
    }

    override fun dispose() {
        autoAim.dispose()
        aiLogicHandler.dispose()
    }


    private fun setTargetForAi(character: Entity, target: Entity) {
        ComponentsMapper.ai.get(character).target = target
        if (ComponentsMapper.aiTurret.has(character)) {
            ComponentsMapper.aiTurret.get(character).target = target
        }
    }


    private val aiEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(Family.all(BaseAiComponent::class.java).get())
    }

}
