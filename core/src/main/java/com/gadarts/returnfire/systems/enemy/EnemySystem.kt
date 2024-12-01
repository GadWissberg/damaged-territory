package com.gadarts.returnfire.systems.enemy

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.EnemyComponent
import com.gadarts.returnfire.components.TurretComponent
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.model.CharacterType
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.CharacterWeaponShotEventData
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt


class EnemySystem : GameEntitySystem() {
    private val cannonSound by lazy { managers.assetsManager.getAssetByDefinition(SoundDefinition.CANNON) }


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
        modelInstanceComponent.gameModelInstance.setBoundingBox(
            managers.assetsManager.getCachedBoundingBox(randomDeadModel)
        )
        EntityBuilder.begin()
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
        if (gameSessionData.gameSessionDataHud.console.isActive) return

        for (turret in enemyTurretEntities) {
            val characterComponent = ComponentsMapper.character.get(ComponentsMapper.turret.get(turret).base)
            if (characterComponent.dead || characterComponent.deathSequenceDuration > 0) continue

            attack(deltaTime, turret)
        }
    }

    private fun attack(
        deltaTime: Float,
        enemy: Entity
    ) {
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(enemy)
        val transform =
            modelInstanceComponent.gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector3_2)
        val player = gameSessionData.player
        val playerPosition =
            ComponentsMapper.modelInstance.get(player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_1
            )
        if (position.dst2(playerPosition) > 90F) return

        val directionToPlayer = auxVector3_3.set(playerPosition).sub(position).nor()
        val currentRotation = transform.getRotation(auxQuat1)

        val forwardDirection =
            auxVector3_4.set(1f, 0f, 0f).rot(auxMatrix.idt().rotate(currentRotation))
        val forwardXZ = auxVector3_5.set(forwardDirection.x, 0f, forwardDirection.z).nor()
        val playerDirectionXZ = auxVector3_6.set(directionToPlayer.x, 0f, directionToPlayer.z).nor()
        val angle = MathUtils.acos(forwardXZ.dot(playerDirectionXZ)) * MathUtils.radiansToDegrees

        val crossY = forwardXZ.crs(playerDirectionXZ)
        val rotationDirection = if (crossY.y > 0) {
            1F
        } else {
            -1F
        }
        val effectiveRotation = min(ROTATION_STEP_SIZE * deltaTime, angle) * rotationDirection

        if (abs(angle - abs(effectiveRotation)) > 6f) {
            transform.setFromEulerAngles(
                currentRotation.yaw + effectiveRotation,
                currentRotation.pitch,
                currentRotation.roll
            ).trn(position)
        } else {
            val roll = MathUtils.atan2(
                directionToPlayer.y,
                sqrt((directionToPlayer.x * directionToPlayer.x + directionToPlayer.z * directionToPlayer.z))
            )
            auxQuat2.idt().setEulerAngles(currentRotation.yaw, currentRotation.pitch, roll * MathUtils.radiansToDegrees)
            if (MathUtils.isEqual(
                    currentRotation.roll,
                    auxQuat2.roll,
                    6F
                )
            ) {
                val enemyComponent = ComponentsMapper.enemy.get(enemy)
                val now = TimeUtils.millis()
                if (enemyComponent.attackReady) {
                    enemyComponent.attackReady = false
                    enemyComponent.attackReadyTime = now + 1000L
                    managers.soundPlayer.play(cannonSound)
                    CharacterWeaponShotEventData.setWithTarget(
                        enemy,
                        player,
                    )
                    managers.dispatcher.dispatchMessage(SystemEvents.CHARACTER_WEAPON_ENGAGED_PRIMARY.ordinal)
                } else if (enemyComponent.attackReadyTime <= now) {
                    enemyComponent.attackReady = true
                }
            } else {
                currentRotation.slerp(auxQuat2, 1F * deltaTime)
                transform.getTranslation(auxVector3_1)
                transform.idt().set(currentRotation).trn(position)
            }
        }
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxVector3_2 = Vector3()
        private val auxVector3_3 = Vector3()
        private val auxVector3_4 = Vector3()
        private val auxVector3_5 = Vector3()
        private val auxVector3_6 = Vector3()
        private val auxQuat1 = Quaternion()
        private val auxQuat2 = Quaternion()
        private val auxMatrix = Matrix4()
        private const val ROTATION_STEP_SIZE = 40F
    }
}
