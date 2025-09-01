package com.gadarts.returnfire.ecs.systems.character.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.MathUtils
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.systems.EntityBuilder
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.data.PhysicsCollisionEventData
import com.gadarts.returnfire.factories.SpecialEffectsFactory
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.managers.SoundManager
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition
import com.gadarts.shared.assets.definitions.SoundDefinition
import com.gadarts.shared.data.definitions.TurretCharacterDefinition
import kotlin.math.max

class CharacterSystemOnPhysicsCollision : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        handleJeepFlagCollision(
            PhysicsCollisionEventData.colObj0.userData as Entity,
            PhysicsCollisionEventData.colObj1.userData as Entity,
        ) || handleJeepFlagCollision(
            PhysicsCollisionEventData.colObj1.userData as Entity,
            PhysicsCollisionEventData.colObj0.userData as Entity,
        ) || handleBulletCharacter(gamePlayManagers, gameSessionData)
                || applyEventForCrashingAircraft(
            PhysicsCollisionEventData.colObj0.userData as Entity,
            PhysicsCollisionEventData.colObj1.userData as Entity,
            gamePlayManagers,
            gameSessionData
        ) || applyEventForCrashingAircraft(
            PhysicsCollisionEventData.colObj1.userData as Entity,
            PhysicsCollisionEventData.colObj0.userData as Entity,
            gamePlayManagers,
            gameSessionData
        )
    }

    private fun handleJeepFlagCollision(
        entity0: Entity,
        entity1: Entity,
    ): Boolean {
        if (!ComponentsMapper.character.has(entity0) || !ComponentsMapper.modelInstance.has(entity1)) return false

        val characterComponent = ComponentsMapper.character.get(entity0)
        val isEntity0Jeep = characterComponent.definition == TurretCharacterDefinition.JEEP
        val flagComponent = ComponentsMapper.flag.get(entity1)
        val isEntity1Flag = flagComponent != null
        if (isEntity0Jeep && isEntity1Flag && flagComponent.follow == null) {
            if (characterComponent.color != flagComponent.color) {
                flagComponent.follow = entity0
            }
            return true
        }
        return false
    }

    private fun handleBulletCharacter(
        gamePlayManagers: GamePlayManagers,
        gameSessionData: GameSessionData
    ) = handleBulletCharacterCollision(
        PhysicsCollisionEventData.colObj0.userData as Entity,
        PhysicsCollisionEventData.colObj1.userData as Entity,
        gamePlayManagers,
        gameSessionData
    ) || handleBulletCharacterCollision(
        PhysicsCollisionEventData.colObj1.userData as Entity,
        PhysicsCollisionEventData.colObj0.userData as Entity,
        gamePlayManagers,
        gameSessionData
    )

    private fun applyEventForCrashingAircraft(
        collider1: Entity,
        collider2: Entity,
        gamePlayManagers: GamePlayManagers,
        gameSessionData: GameSessionData
    ): Boolean {
        val crashSoundEmitter = ComponentsMapper.crashingAircraftEmitter.get(collider1)
        if (crashSoundEmitter != null
            && !crashSoundEmitter.crashed
            && (ComponentsMapper.ground.has(collider2) || ComponentsMapper.amb.has(collider2))
        ) {
            if (ComponentsMapper.physics.get(collider1).rigidBody.linearVelocity.len2() >= 9F) {
                generateExplosion(gamePlayManagers, collider1)
                val position =
                    ComponentsMapper.modelInstance.get(collider1).gameModelInstance.modelInstance.transform.getTranslation(
                        auxVector
                    )
                val assetsManager = gamePlayManagers.assetsManager
                gamePlayManagers.soundManager.play(
                    assetsManager.getAssetByDefinition(SoundDefinition.CRASH_BIG),
                    position
                )
                val particleEffectsPools = gameSessionData.gamePlayData.pools.particleEffectsPools
                gamePlayManagers.ecs.entityBuilder.begin().addParticleEffectComponent(
                    position,
                    particleEffectsPools.obtain(ParticleEffectDefinition.EXPLOSION)
                ).finishAndAddToEngine()
                gamePlayManagers.ecs.entityBuilder.begin()
                    .addParticleEffectComponent(position, particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE))
                    .finishAndAddToEngine()
                crashSoundEmitter.crash()
                crashSoundEmitter.soundToStop.stop(crashSoundEmitter.soundToStopId)
                gamePlayManagers.stainsHandler.addCrate(position)
            }
            return true
        }
        return false
    }

    private fun generateExplosion(
        gamePlayManagers: GamePlayManagers,
        entity: Entity
    ) {
        gamePlayManagers.factories.specialEffectsFactory.generateExplosion(
            entity = entity,
            blastRing = true,
            addBiasToPosition = false
        )
    }

    private fun handleBulletCharacterCollision(
        first: Entity,
        second: Entity,
        gamePlayManagers: GamePlayManagers,
        gameSessionData: GameSessionData
    ): Boolean {
        val isSecondCharacter = ComponentsMapper.character.has(second)
        val isSecondTurret = if (!isSecondCharacter) ComponentsMapper.turret.has(second) else false
        if (ComponentsMapper.bullet.has(first) && !ComponentsMapper.bullet.get(first).destroyed && (isSecondCharacter || isSecondTurret)) {
            val damage = max(ComponentsMapper.bullet.get(first).damage + MathUtils.random(-2F, 2F), 1F)
            if (damage >= 8F) {
                gamePlayManagers.soundManager.play(
                    gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.MAJOR_IMPACT),
                    ComponentsMapper.modelInstance.get(first).gameModelInstance.modelInstance.transform.getTranslation(
                        auxVector
                    )
                )
            }
            val entity = if (isSecondCharacter) {
                second
            } else {
                ComponentsMapper.turret.get(second).base
            }
            val damagedCharacterComponent = ComponentsMapper.character.get(entity)
            val beforeDamage = damagedCharacterComponent.hp
            damagedCharacterComponent.takeDamage(damage)
            val afterDamage = damagedCharacterComponent.hp
            applyPainExplosionEffect(
                entity,
                beforeDamage,
                afterDamage,
                gamePlayManagers,
                gameSessionData,
            )
            addSpark(gamePlayManagers, first, gameSessionData)
            return true
        }
        return false
    }

    private fun addSpark(
        gamePlayManagers: GamePlayManagers,
        first: Entity,
        gameSessionData: GameSessionData
    ) {
        gamePlayManagers.ecs.entityBuilder.begin()
            .addParticleEffectComponent(
                ComponentsMapper.modelInstance.get(first).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector
                ),
                gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SPARK_MED)
            )
            .finishAndAddToEngine()
    }

    private fun applyPainExplosionEffect(
        character: Entity,
        beforeDamage: Float,
        afterDamage: Float,
        gamePlayManagers: GamePlayManagers,
        gameSessionData: GameSessionData,
    ) {
        if (afterDamage <= 0) return

        val damagedCharacter = ComponentsMapper.character.get(character)
        val initialHp = damagedCharacter.definition.getHP()
        val entityBuilder = gamePlayManagers.ecs.entityBuilder
        val specialEffectsFactory = gamePlayManagers.factories.specialEffectsFactory
        val soundPlayer = gamePlayManagers.soundManager
        val assetsManager = gamePlayManagers.assetsManager
        if (damagedCharacter.creationTime / 2 == 0L) {
            val half = initialHp / 2F
            if (beforeDamage >= half && afterDamage < half) {
                createPainExplosionEffect(
                    entityBuilder,
                    character,
                    gameSessionData,
                    specialEffectsFactory,
                    soundPlayer,
                    assetsManager
                )
            }
        } else {
            val third = initialHp / 3
            if (afterDamage <= (initialHp - third) && beforeDamage.toInt() / third.toInt() != afterDamage.toInt() / third.toInt()) {
                createPainExplosionEffect(
                    entityBuilder,
                    character,
                    gameSessionData,
                    specialEffectsFactory,
                    soundPlayer,
                    assetsManager
                )
            }
        }
    }

    private fun createPainExplosionEffect(
        entityBuilder: EntityBuilder,
        character: Entity,
        gameSessionData: GameSessionData,
        specialEffectsFactory: SpecialEffectsFactory,
        soundManager: SoundManager,
        assetsManager: GameAssetManager
    ) {
        val turretBaseComponent = ComponentsMapper.turretBase.get(character)
        val entity = turretBaseComponent?.turret ?: character
        val transform = ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform
        val position =
            transform.getTranslation(
                auxVector
            )
        entityBuilder.begin().addParticleEffectComponent(
            auxVector2.set(position).add(
                MathUtils.random(-MAX_PAIN_EXPLOSION_BIAS, MAX_PAIN_EXPLOSION_BIAS),
                MathUtils.random(-MAX_PAIN_EXPLOSION_BIAS, MAX_PAIN_EXPLOSION_BIAS),
                MathUtils.random(-MAX_PAIN_EXPLOSION_BIAS, MAX_PAIN_EXPLOSION_BIAS)
            ),
            gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.EXPLOSION)
        ).finishAndAddToEngine()
        soundManager.play(
            assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION),
            position
        )
        specialEffectsFactory.generateSmallFlyingParts(position)
    }

    companion object {
        private val auxVector = com.badlogic.gdx.math.Vector3()
        private val auxVector2 = com.badlogic.gdx.math.Vector3()
        private const val MAX_PAIN_EXPLOSION_BIAS = 0.2F
    }
}
