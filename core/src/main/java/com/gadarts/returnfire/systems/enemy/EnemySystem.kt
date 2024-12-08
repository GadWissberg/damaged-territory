package com.gadarts.returnfire.systems.enemy

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.EnemyComponent
import com.gadarts.returnfire.components.TurretComponent
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.model.CharacterType
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents


class EnemySystem(managers: Managers) : GameEntitySystem(managers) {
    private val enemyAi by lazy { EnemyAttackLogic(gameSessionData, this.managers) }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.CHARACTER_DIED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                val entity = msg.extraInfo as Entity
                val characterComponent = ComponentsMapper.character.get(entity)
                if (characterComponent.definition.getCharacterType() == CharacterType.TURRET
                    && ComponentsMapper.enemy.has(entity)
                ) {
                    destroyTurret(entity, managers, gameSessionData)
                }
            }

        }
    )

    private fun destroyTurret(
        entity: Entity,
        managers: Managers,
        gameSessionData: GameSessionData
    ) {
        val modelInstanceComponent =
            ComponentsMapper.modelInstance.get(ComponentsMapper.turretBase.get(entity).turret)
        auxMatrix.set(modelInstanceComponent.gameModelInstance.modelInstance.transform)
        val transform = modelInstanceComponent.gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector3_1)
        val randomDeadModel =
            if (MathUtils.randomBoolean()) ModelDefinition.TURRET_CANNON_DEAD_0 else ModelDefinition.TURRET_CANNON_DEAD_1
        modelInstanceComponent.gameModelInstance = GameModelInstance(
            ModelInstance(managers.assetsManager.getAssetByDefinition(randomDeadModel)),
            ModelDefinition.TURRET_CANNON_DEAD_0,
        )
        modelInstanceComponent.gameModelInstance.modelInstance.transform.set(auxMatrix)
        modelInstanceComponent.gameModelInstance.setBoundingBox(
            managers.assetsManager.getCachedBoundingBox(randomDeadModel)
        )
        managers.entityBuilder.begin()
            .addParticleEffectComponent(
                position,
                gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.EXPLOSION)
            )
            .finishAndAddToEngine()
        managers.soundPlayer.play(
            managers.assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION),
        )
    }


    private val enemyTurretEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(Family.all(TurretComponent::class.java, EnemyComponent::class.java).get())
    }


    override fun update(deltaTime: Float) {
        if (gameSessionData.hudData.console.isActive) return

        for (turret in enemyTurretEntities) {
            val characterComponent = ComponentsMapper.character.get(ComponentsMapper.turret.get(turret).base)
            if (characterComponent.dead || characterComponent.deathSequenceDuration > 0) continue

            enemyAi.attack(deltaTime, turret)
        }
    }


    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxMatrix = Matrix4()
    }
}
