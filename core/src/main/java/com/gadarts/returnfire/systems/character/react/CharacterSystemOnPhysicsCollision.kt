package com.gadarts.returnfire.systems.character.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.MathUtils
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.factories.SpecialEffectsFactory
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.managers.SoundPlayer
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData
import kotlin.math.max

class CharacterSystemOnPhysicsCollision : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        handleBulletCharacterCollision(
            PhysicsCollisionEventData.colObj0.userData as Entity,
            PhysicsCollisionEventData.colObj1.userData as Entity,
            gamePlayManagers,
            gameSessionData
        ) || handleBulletCharacterCollision(
            PhysicsCollisionEventData.colObj1.userData as Entity,
            PhysicsCollisionEventData.colObj0.userData as Entity,
            gamePlayManagers,
            gameSessionData
        ) || applyEventForCrashingAircraft(
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

    private fun applyEventForCrashingAircraft(
        collider1: Entity,
        collider2: Entity,
        gamePlayManagers: GamePlayManagers,
        gameSessionData: GameSessionData
    ): Boolean {
        val crashSoundEmitter = ComponentsMapper.crashingAircraftEmitter.get(collider1)
        if (crashSoundEmitter != null && !crashSoundEmitter.crashed && ComponentsMapper.ground.has(collider2)) {
            if (ComponentsMapper.physics.get(collider1).rigidBody.linearVelocity.len2() >= 9F) {
                gamePlayManagers.factories.specialEffectsFactory.generateExplosion(
                    entity = collider1,
                    blastRing = true,
                    addBiasToPosition = false
                )
                val modelInstance = ComponentsMapper.modelInstance.get(collider1).gameModelInstance.modelInstance
                val position = modelInstance.transform.getTranslation(auxVector)
                gamePlayManagers.soundPlayer.play(
                    gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.CRASH_BIG),
                    position
                )
                gamePlayManagers.ecs.entityBuilder.begin().addParticleEffectComponent(
                    position,
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE)
                ).finishAndAddToEngine()
                crashSoundEmitter.crash()
                crashSoundEmitter.soundToStop.stop(crashSoundEmitter.soundToStopId)
            }
            return true
        }
        return false
    }

    private fun handleBulletCharacterCollision(
        first: Entity,
        second: Entity,
        gamePlayManagers: GamePlayManagers,
        gameSessionData: GameSessionData
    ): Boolean {
        val isSecondCharacter = ComponentsMapper.character.has(second)
        val isSecondTurret = if (!isSecondCharacter) ComponentsMapper.turret.has(second) else false
        if (ComponentsMapper.bullet.has(first) && (isSecondCharacter || isSecondTurret)) {
            val damage = max(ComponentsMapper.bullet.get(first).damage + MathUtils.random(-2F, 2F), 1F)
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
        val soundPlayer = gamePlayManagers.soundPlayer
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
        soundPlayer: SoundPlayer,
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
        soundPlayer.play(
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
