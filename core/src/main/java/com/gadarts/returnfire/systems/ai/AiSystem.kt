package com.gadarts.returnfire.systems.ai

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.ai.AiComponent
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.turret.TurretComponent
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.ai.logic.AiLogicHandler
import com.gadarts.returnfire.systems.ai.logic.TurretLogic
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.physics.BulletEngineHandler


class AiSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {
    private val aiLogicHandler: AiLogicHandler by lazy {
        AiLogicHandler(
            gameSessionData, gamePlayManagers, autoAim, engine.getEntitiesFor(
                Family.all(AiComponent::class.java)
                    .get()
            )
        )
    }
    private val autoAim by lazy {
        gamePlayManagers.factories.autoAimShapeFactory.generate(
            BulletEngineHandler.COLLISION_GROUP_AI,
            BulletEngineHandler.COLLISION_GROUP_PLAYER,
        )
    }


    private val turretLogic by lazy { TurretLogic(gameSessionData, this.gamePlayManagers) }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(

        SystemEvents.OPPONENT_CHARACTER_CREATED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                val entity = msg.extraInfo as Entity
                val characterComponent = ComponentsMapper.character.get(entity)
                if (characterComponent.color == CharacterColor.GREEN) {
                    gamePlayManagers.ecs.entityBuilder.addAiComponentToEntity(
                        entity,
                        characterComponent.definition.getHP()
                    )
                    val turretBaseComponent = ComponentsMapper.turretBase.get(entity)
                    if (turretBaseComponent != null) {
                        gamePlayManagers.ecs.entityBuilder.addAiTurretComponentToEntity(
                            turretBaseComponent.turret,
                        )
                    }
                }
            }
        },
    )


    private val enemyTurretEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(Family.all(TurretComponent::class.java, AiComponent::class.java).get())
    }


    override fun update(deltaTime: Float) {
        if (GameDebugSettings.AI_DISABLED
            || gameSessionData.hudData.console.isActive
            || gameSessionData.gamePlayData.player == null
            || ComponentsMapper.boarding.get(gameSessionData.gamePlayData.player).isBoarding()
        ) return

        for (turret in enemyTurretEntities) {
            val characterComponent = ComponentsMapper.character.get(ComponentsMapper.turret.get(turret).base)
            if (characterComponent == null || characterComponent.dead || ComponentsMapper.deathSequence.has(turret)) continue

            turretLogic.attack(deltaTime, turret)
        }
        aiLogicHandler.update(deltaTime)
    }


    override fun resume(delta: Long) {
    }

    override fun dispose() {
        autoAim.dispose()
        aiLogicHandler.dispose()
    }

}
