package com.gadarts.returnfire.systems.character.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.MathUtils
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
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
        val canPlay =
            crashSoundEmitter != null && !crashSoundEmitter.crashed
        if (canPlay && ComponentsMapper.ground.has(collider2)) {
            val velocity = ComponentsMapper.physics.get(collider1).rigidBody.linearVelocity.len2()
            if (velocity >= 9F) {
                val modelInstance = ComponentsMapper.modelInstance.get(collider1).gameModelInstance.modelInstance
                val position = modelInstance.transform.getTranslation(auxVector)
                gamePlayManagers.soundPlayer.play(
                    gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.CRASH_BIG),
                    position
                )
                gamePlayManagers.ecs.entityBuilder.begin().addParticleEffectComponent(
                    position,
                    gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE)
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
            val damage = max(ComponentsMapper.bullet.get(first).damage + MathUtils.random(-2, 2), 1)
            val damagedCharacter = if (isSecondCharacter) {
                ComponentsMapper.character.get(second)
            } else {
                ComponentsMapper.character.get(ComponentsMapper.turret.get(second).base)
            }
            damagedCharacter.takeDamage(damage)
            gamePlayManagers.ecs.entityBuilder.begin()
                .addParticleEffectComponent(
                    ComponentsMapper.modelInstance.get(first).gameModelInstance.modelInstance.transform.getTranslation(
                        auxVector
                    ),
                    gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.RICOCHET)
                )
                .finishAndAddToEngine()
            return true
        }
        return false
    }

    companion object {
        private val auxVector = com.badlogic.gdx.math.Vector3()
    }
}
