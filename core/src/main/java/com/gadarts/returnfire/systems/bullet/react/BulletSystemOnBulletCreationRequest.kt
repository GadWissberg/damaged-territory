package com.gadarts.returnfire.systems.bullet.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseProxy
import com.badlogic.gdx.physics.bullet.collision.btSphereShape
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.data.BulletCreationRequestEventData

class BulletSystemOnBulletCreationRequest : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
        val armComponent = BulletCreationRequestEventData.armComponent
        val armProperties = armComponent.armProperties
        managers.soundPlayer.playPositionalSound(
            armProperties.shootingSound,
            gameSessionData.player,
            gameSessionData.renderData.camera,
            randomPitch = false,
        )
        val spark = armComponent.spark
        val parentTransform =
            ComponentsMapper.modelInstance.get(ComponentsMapper.spark.get(spark).parent).gameModelInstance.modelInstance.transform
        val position = parentTransform.getTranslation(auxVector1)
        position.add(BulletCreationRequestEventData.relativePosition)
        showSpark(spark, position, parentTransform)
        val gameModelInstance =
            gameSessionData.pools.gameModelInstancePools[armProperties.modelDefinition]!!.obtain()
        val entityBuilder = EntityBuilder.begin()
            .addModelInstanceComponent(gameModelInstance, position, armProperties.boundingBox)
            .addBulletComponent(
                armComponent.behavior,
                armProperties.explosion,
                armProperties.explosive,
                BulletCreationRequestEventData.friendly,
                armProperties.damage
            )
        addSmokeTrail(armComponent, entityBuilder, position)
        val bullet = entityBuilder.finishAndAddToEngine()
        applyPhysicsToBullet(
            bullet,
            gameModelInstance,
            BulletCreationRequestEventData.direction,
            armProperties,
            managers.dispatcher
        )
        addSmokeEmission(armProperties, gameModelInstance, position)
        addSparkParticleEffect(position, armComponent)
    }

    private fun addSparkParticleEffect(position: Vector3, arm: ArmComponent) {
        EntityBuilder.begin()
            .addParticleEffectComponent(
                position,
                arm.armProperties.sparkParticleEffect
            )
            .finishAndAddToEngine()
    }

    private fun showSpark(
        spark: Entity,
        position: Vector3?,
        parentTransform: Matrix4
    ) {
        val sparkModelInstanceComponent = ComponentsMapper.modelInstance.get(spark)
        val sparkTransform = sparkModelInstanceComponent.gameModelInstance.modelInstance.transform
        sparkTransform.setToTranslation(position).rotate(parentTransform.getRotation(auxQuat))
            .rotate(Vector3.Z, -30F)
        sparkTransform.rotate(
            Vector3.X, MathUtils.random(360F)
        )
        sparkModelInstanceComponent.hideAt = TimeUtils.millis() + 50L
        sparkModelInstanceComponent.hidden = false
    }

    private fun addSmokeEmission(
        armProperties: ArmProperties,
        gameModelInstance: GameModelInstance,
        position: Vector3
    ) {
        if (armProperties.smokeEmit != null) {
            val yaw = gameModelInstance.modelInstance.transform.getRotation(
                auxQuat
            ).yaw
            EntityBuilder.begin()
                .addParticleEffectComponent(
                    auxVector3.set(position).sub(
                        auxVector2.set(Vector3.X).rotate(
                            Vector3.Y,
                            yaw
                        ).scl(0.25F)
                    ),
                    armProperties.smokeEmit,
                    yaw
                ).finishAndAddToEngine()
        }
    }

    private fun applyPhysicsToBullet(
        bullet: Entity,
        gameModelInstance: GameModelInstance,
        aimingTransform: Matrix4,
        armProperties: ArmProperties,
        dispatcher: MessageDispatcher
    ) {
        val shape = btSphereShape(armProperties.radius)
        EntityBuilder.addPhysicsComponent(
            shape,
            bullet,
            gameModelInstance.modelInstance.transform,
            0.5F,
            dispatcher,
        )
        gameModelInstance.modelInstance.transform.rotate(aimingTransform.getRotation(auxQuat)).rotate(
            Vector3.Z,
            armProperties.initialRotationAroundZ
        )
        val physicsComponent = ComponentsMapper.physics.get(bullet)
        physicsComponent.rigidBody.linearVelocity =
            gameModelInstance.modelInstance.transform.getRotation(auxQuat)
                .transform(auxVector2.set(1F, 0F, 0F))
                .scl(armProperties.speed)
        physicsComponent.rigidBody.worldTransform = gameModelInstance.modelInstance.transform
        physicsComponent.rigidBody.gravity = Vector3.Zero
        physicsComponent.rigidBody.contactCallbackFilter =
            btBroadphaseProxy.CollisionFilterGroups.AllFilter
    }

    private fun addSmokeTrail(
        arm: ArmComponent,
        entityBuilder: EntityBuilder,
        position: Vector3
    ) {
        if (arm.armProperties.smokeTrail != null) {
            entityBuilder
                .addParticleEffectComponent(
                    position = position,
                    pool = arm.armProperties.smokeTrail!!,
                    thisEntityAsParent = true
                )
        }
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxQuat = Quaternion()
    }
}
