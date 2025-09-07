package com.gadarts.returnfire.ecs.systems.ai.logic

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.ai.BaseAiComponent
import com.gadarts.returnfire.ecs.components.turret.TurretComponent
import com.gadarts.returnfire.ecs.systems.ai.AiGoals
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.TurretCharacterDefinition

class AiCharacterLogicHandler(
    private val gameSessionData: GameSessionData,
    private val aiCharacterEntities: ImmutableArray<Entity>,
    gamePlayManagers: GamePlayManagers,
    autoAim: btPairCachingGhostObject,
    engine: Engine
) : Disposable {
    private var goal: AiGoals = AiGoals.CLEAR_WAY_TO_RIVAL_FLAG

    private val enemyTurretEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(Family.all(TurretComponent::class.java, BaseAiComponent::class.java).get())
    }
    private val aiEnemyTurretLogic by lazy { AiEnemyTurretLogic(gameSessionData, gamePlayManagers) }

    private val logics = mapOf(
        SimpleCharacterDefinition.APACHE to AiApacheLogic(
            gameSessionData,
            gamePlayManagers.dispatcher,
            gamePlayManagers.soundManager,
            gamePlayManagers.assetsManager,
            autoAim
        ),
        TurretCharacterDefinition.TANK to AiGroundCharacterLogic(
            gameSessionData,
            autoAim,
            gamePlayManagers
        ),
        TurretCharacterDefinition.JEEP to AiGroundCharacterLogic(
            gameSessionData,
            autoAim,
            gamePlayManagers
        )
    )

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

            val logic = logics[characterComponent.definition]
            if (logic != null) {
                updateLogic(character, deltaTime, logic)
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
        logics.forEach { (_, aiLogic) ->
            aiLogic.dispose()
        }
    }

    fun onCharacterCreated(character: Entity) {
        if (goal == AiGoals.CLEAR_WAY_TO_RIVAL_FLAG) {
            val rivalFlag = gameSessionData.gamePlayData.flags[CharacterColor.BROWN]
            val baseAiComponent = ComponentsMapper.ai.get(character)
            if (rivalFlag != null) {
                baseAiComponent.target = rivalFlag
                if (ComponentsMapper.aiTurret.has(character)) {
                    ComponentsMapper.aiTurret.get(character).target = rivalFlag
                }
            }
            baseAiComponent.goal = AiGoals.CLEAR_WAY_TO_RIVAL_FLAG
        }
    }
}
