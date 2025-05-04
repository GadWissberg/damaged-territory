package com.gadarts.returnfire.systems.ai.logic

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.ai.BaseAiComponent
import com.gadarts.returnfire.components.turret.TurretComponent
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.definitions.SimpleCharacterDefinition
import com.gadarts.returnfire.model.definitions.TurretCharacterDefinition
import com.gadarts.returnfire.systems.data.GameSessionData

class AiLogicHandler(
    gameSessionData: GameSessionData,
    gamePlayManagers: GamePlayManagers,
    autoAim: btPairCachingGhostObject,
    private val aiCharacterEntities: ImmutableArray<Entity>,
    engine: Engine
) : Disposable {
    private val enemyTurretEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(Family.all(TurretComponent::class.java, BaseAiComponent::class.java).get())
    }
    private val aiEnemyTurretLogic by lazy { AiEnemyTurretLogic(gameSessionData, gamePlayManagers) }

    private val aiApacheLogic by lazy {
        AiApacheLogic(
            gameSessionData,
            gamePlayManagers.dispatcher,
            gamePlayManagers.ecs.entityBuilder,
            gamePlayManagers.soundManager,
            gamePlayManagers.assetsManager,
            autoAim
        )
    }
    private val aiTankLogic by lazy {
        AiTankLogic(
            gameSessionData,
            autoAim,
            gamePlayManagers
        )
    }

    fun update(deltaTime: Float) {
        updateEnemyTurrets(deltaTime)
        for (i in 0 until aiCharacterEntities.size()) {
            val character = aiCharacterEntities[i]
            val boardingComponent = ComponentsMapper.boarding.get(character)
            val characterComponent = ComponentsMapper.character.get(
                character
            )


            if ((boardingComponent != null && boardingComponent.isBoarding())
                || characterComponent == null
                || characterComponent.dead
            ) continue

            if (characterComponent.definition == SimpleCharacterDefinition.APACHE) {
                updateLogic(character, deltaTime, aiApacheLogic)
            } else if (characterComponent.definition == TurretCharacterDefinition.TANK) {
                updateLogic(character, deltaTime, aiTankLogic)
            }
        }
    }

    private fun updateEnemyTurrets(deltaTime: Float) {
        for (turret in enemyTurretEntities) {
            val characterComponent = ComponentsMapper.character.get(ComponentsMapper.turret.get(turret).base)
            if (characterComponent == null || characterComponent.dead || ComponentsMapper.deathSequence.has(turret)) continue

            aiEnemyTurretLogic.attack(deltaTime, turret)
        }
    }

    private fun updateLogic(character: Entity, deltaTime: Float, logic: AiCharacterLogic) {
        logic.preUpdate(character, deltaTime)
        logic.update(character, deltaTime)
    }

    override fun dispose() {
        aiTankLogic.dispose()
    }
}
