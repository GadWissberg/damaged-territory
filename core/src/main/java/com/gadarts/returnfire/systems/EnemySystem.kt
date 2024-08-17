package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.EnemyComponent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData
import kotlin.math.abs
import kotlin.math.min


class EnemySystem : GameEntitySystem() {
    private val explosionSound by lazy { managers.assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION) }
    private val cannonSound by lazy { managers.assetsManager.getAssetByDefinition(SoundDefinition.CANNON) }
    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.PHYSICS_COLLISION to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                managers: Managers
            ) {
                val entity0 = PhysicsCollisionEventData.colObj0.userData as Entity
                val entity1 = PhysicsCollisionEventData.colObj1.userData as Entity
                onCollisionEnemyAndBullet(
                    entity0,
                    entity1
                ) || onCollisionEnemyAndBullet(
                    entity1,
                    entity0
                )
            }

        }
    )

    private fun onCollisionEnemyAndBullet(entity0: Entity, entity1: Entity): Boolean {
        if (ComponentsMapper.bullet.has(entity0) && ComponentsMapper.enemy.has(entity1)) {
            ComponentsMapper.enemy.get(entity1).dead = true
            val rigidBody = ComponentsMapper.physics.get(entity1).rigidBody
            rigidBody.gravity = auxVector1.set(0F, -9.8F, 0F)
            rigidBody.collisionFlags = CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK
            rigidBody.applyCentralImpulse(
                auxVector1.set(Vector3.X).rotate(Vector3.Z, 90F)
                    .rotate(Vector3.Y, MathUtils.random(360F)).nor().scl(64F),
            )
            managers.soundPlayer.play(explosionSound)
            return true
        }
        return false
    }

    private val auxRay by lazy {
        val closestRayResultCallback = ClosestRayResultCallback(Vector3(), Vector3())
        closestRayResultCallback.collisionFilterGroup = -1
        closestRayResultCallback.collisionFilterMask = -1
        closestRayResultCallback
    }
    private val explosionParticleEffect by lazy {
        managers.assetsManager.getAssetByDefinition(ParticleEffectDefinition.EXPLOSION_GROUND)
    }
    private val sparkDecalTextureRegion by lazy {
        TextureRegion(managers.assetsManager.getTexture("spark"))
    }

    private val enemyEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(Family.all(EnemyComponent::class.java).get())
    }


    override fun update(deltaTime: Float) {
        for (enemy in enemyEntities) {
            if (ComponentsMapper.enemy.get(enemy).dead) continue

            val transform =
                ComponentsMapper.modelInstance.get(enemy).gameModelInstance.modelInstance.transform

            attack(transform, deltaTime, enemy)
        }
    }

    private fun attack(
        transform: Matrix4,
        deltaTime: Float,
        enemy: Entity
    ) {
        val position = transform.getTranslation(auxVector2)
        val playerPosition =
            ComponentsMapper.modelInstance.get(gameSessionData.player).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        if (position.dst2(playerPosition) > 90F) return

        val directionToPlayer = auxVector3.set(playerPosition).sub(position).nor()
        val currentRotation = transform.getRotation(auxQuat)

        val forwardDirection =
            auxVector4.set(1f, 0f, 0f).rot(auxMatrix.idt().rotate(currentRotation))
        val forwardXZ = auxVector5.set(forwardDirection.x, 0f, forwardDirection.z).nor()
        val playerDirectionXZ = auxVector6.set(directionToPlayer.x, 0f, directionToPlayer.z).nor()
        val angle = MathUtils.acos(forwardXZ.dot(playerDirectionXZ)) * MathUtils.radiansToDegrees

        val crossY = forwardXZ.crs(playerDirectionXZ)
        val rotationDirection = if (crossY.y > 0) {
            1F
        } else {
            -1F
        }
        val effectiveRotation = min(ROTATION_STEP_SIZE * deltaTime, angle) * rotationDirection

        if (abs(angle - abs(effectiveRotation)) > 0.1f) {
            transform.setFromEulerAngles(
                currentRotation.yaw + effectiveRotation,
                currentRotation.pitch,
                currentRotation.roll
            ).trn(position)
        } else {
            val enemyComponent = ComponentsMapper.enemy.get(enemy)
            val now = TimeUtils.millis()
            if (enemyComponent.attackReady) {
                enemyComponent.attackReady = false
                enemyComponent.attackReadyTime = now + 3000L
                gameSessionData.gameSessionPhysicsData.collisionWorld.rayTest(
                    position,
                    playerPosition,
                    auxRay
                )
                createSpark(transform, position, 1F)
                createSpark(transform, position, -1F)
                managers.soundPlayer.play(cannonSound)
                val collisionObject = auxRay.collisionObject
                if (collisionObject != null) {
                    val collisionEntity = collisionObject.userData as Entity
                    if (ComponentsMapper.player.has(collisionEntity)) {
                        EntityBuilder.begin()
                            .addParticleEffectComponent(explosionParticleEffect, playerPosition)
                            .finishAndAddToEngine()
                    }
                }
            } else if (enemyComponent.attackReadyTime <= now) {
                enemyComponent.attackReady = true
            }
        }
    }

    private fun createSpark(transform: Matrix4, position: Vector3, side: Float) {
        EntityBuilder.begin().addIndependentDecalComponent(
            sparkDecalTextureRegion,
            100L,
            auxVector4.setZero().add(0.7F, 0F, side * 0.3F).rot(transform).add(position)
        ).finishAndAddToEngine()
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
        auxRay.dispose()
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxVector4 = Vector3()
        private val auxVector5 = Vector3()
        private val auxVector6 = Vector3()
        private val auxQuat = Quaternion()
        private val auxMatrix = Matrix4()
        private const val ROTATION_STEP_SIZE = 40F
    }
}
