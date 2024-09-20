package com.gadarts.returnfire.systems.bullet

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseProxy
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.BulletCreationRequestEventData
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData

class BulletSystem : GameEntitySystem() {
    private val waterSplashSounds by lazy {
        managers.assetsManager.getAllAssetsByDefinition(
            SoundDefinition.WATER_SPLASH
        )
    }
    private val waterSplashFloorTexture: Texture by lazy { managers.assetsManager.getTexture("water_splash_floor") }
    private val blastRingTexture: Texture by lazy { managers.assetsManager.getTexture("blast_ring") }
    private val bulletEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(BulletComponent::class.java).get()
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.PHYSICS_COLLISION to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                managers: Managers
            ) {
                handleBulletCollision(
                    PhysicsCollisionEventData.colObj0.userData as Entity,
                    PhysicsCollisionEventData.colObj1.userData as Entity
                ) || handleBulletCollision(
                    PhysicsCollisionEventData.colObj1.userData as Entity,
                    PhysicsCollisionEventData.colObj0.userData as Entity
                )

            }
        },
        SystemEvents.BULLET_CREATION_REQUEST to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                createBullet()
            }

        }
    )

    private fun createBullet() {
        val armComponent = BulletCreationRequestEventData.armComponent
        val armProperties = armComponent.armProperties
        managers.soundPlayer.play(
            armProperties.shootingSound,
        )
        val spark = armComponent.spark
        val parentTransform =
            ComponentsMapper.modelInstance.get(ComponentsMapper.spark.get(spark).parent).gameModelInstance.modelInstance.transform
        val position = parentTransform.getTranslation(auxVector1)
        position.add(BulletCreationRequestEventData.relativePosition)
        showSpark(spark, position, parentTransform)
        val renderData = armProperties.renderData
        val gameModelInstance =
            gameSessionData.pools.gameModelInstancePools[renderData.modelDefinition]!!.obtain()
        val entityBuilder = EntityBuilder.begin()
            .addModelInstanceComponent(gameModelInstance, position, renderData.boundingBox)
            .addBulletComponent(
                armComponent.behavior,
                armProperties.effectsData.explosion,
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
        )
        addSmokeEmission(armProperties, gameModelInstance, position)
        addSparkParticleEffect(position, armComponent)
    }

    private fun addSparkParticleEffect(position: Vector3, arm: ArmComponent) {
        EntityBuilder.begin()
            .addParticleEffectComponent(
                position,
                arm.armProperties.effectsData.sparkParticleEffect
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
        val effectsData = armProperties.effectsData
        if (effectsData.smokeEmit != null) {
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
                    effectsData.smokeEmit,
                    yaw
                ).finishAndAddToEngine()
        }
    }

    private fun applyPhysicsToBullet(
        bullet: Entity,
        gameModelInstance: GameModelInstance,
        aimingTransform: Matrix4,
        armProperties: ArmProperties,
    ) {
        val transform = gameModelInstance.modelInstance.transform
        EntityBuilder.addPhysicsComponentPooled(
            bullet,
            armProperties.rigidBodyPool,
            managers.dispatcher,
            CollisionFlags.CF_CHARACTER_OBJECT,
            transform,
        )
        transform.rotate(aimingTransform.getRotation(auxQuat)).rotate(
            Vector3.Z,
            armProperties.renderData.initialRotationAroundZ
        )
        val physicsComponent = ComponentsMapper.physics.get(bullet)
        physicsComponent.rigidBody.linearVelocity =
            transform.getRotation(auxQuat)
                .transform(auxVector2.set(1F, 0F, 0F))
                .scl(armProperties.speed)
        physicsComponent.rigidBody.worldTransform = transform
        physicsComponent.rigidBody.gravity = Vector3.Zero
        physicsComponent.rigidBody.contactCallbackFilter =
            btBroadphaseProxy.CollisionFilterGroups.AllFilter
    }

    private fun addSmokeTrail(
        arm: ArmComponent,
        entityBuilder: EntityBuilder,
        position: Vector3
    ) {
        val effectsData = arm.armProperties.effectsData
        if (effectsData.smokeTrail != null) {
            entityBuilder
                .addParticleEffectComponent(
                    position = position,
                    pool = effectsData.smokeTrail,
                    thisEntityAsParent = true
                )
        }
    }

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        engine.addEntityListener(object : EntityListener {
            override fun entityAdded(entity: Entity) {

            }

            override fun entityRemoved(entity: Entity) {
                if (ComponentsMapper.bullet.has(entity)) {
                    gameSessionData.pools.gameModelInstancePools[ComponentsMapper.modelInstance.get(
                        entity
                    ).gameModelInstance.definition]
                        ?.free(
                            ComponentsMapper.modelInstance.get(
                                entity
                            ).gameModelInstance
                        )
                    val rigidBody = ComponentsMapper.physics.get(entity).rigidBody
                    rigidBody.rigidBodyPool?.free(rigidBody)
                }
            }

        })
    }

    private fun handleBulletCollision(entity0: Entity, entity1: Entity): Boolean {
        val isBullet = ComponentsMapper.bullet.has(entity0)
        if (isBullet) {
            val position =
                ComponentsMapper.modelInstance.get(entity0).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            val tileEntity = gameSessionData.tilesEntities[position.z.toInt()][position.x.toInt()]
            if (ComponentsMapper.ground.has(entity1) && tileEntity != null && ComponentsMapper.ground.get(
                    tileEntity
                ).water
            ) {
                addWaterSplash(position)
            } else {
                addBulletExplosion(entity0, position)
            }
            destroyBullet(entity0)
            if (ComponentsMapper.bullet.has(entity1)) {
                destroyBullet(entity1)
            }
            return true
        }
        return false
    }

    private fun addGroundBlast(
        position: Vector3,
        texture: Texture,
        startingScale: Float,
        scalePace: Float,
        duration: Int,
        fadeOutPace: Float
    ) {
        val gameModelInstance = gameSessionData.groundBlastPool.obtain()
        val modelInstance = gameModelInstance.modelInstance
        modelInstance.transform.setToScaling(1F, 1F, 1F)
        val material = modelInstance.materials.get(0)
        val blendingAttribute = material.get(BlendingAttribute.Type) as BlendingAttribute
        blendingAttribute.opacity = 1F
        val textureAttribute =
            material.get(TextureAttribute.Diffuse) as TextureAttribute
        textureAttribute.textureDescription.texture = texture
        EntityBuilder.begin()
            .addModelInstanceComponent(
                gameModelInstance,
                position,
                auxBoundingBox.ext(Vector3.Zero, 1F)
            )
            .addGroundBlastComponent(scalePace, duration, fadeOutPace)
            .finishAndAddToEngine()
        modelInstance.transform.scl(startingScale)
    }

    private fun addBulletExplosion(bullet: Entity, position: Vector3) {
        val bulletComponent = ComponentsMapper.bullet.get(bullet)
        val explosion = bulletComponent.explosion
        val explosive = bulletComponent.explosive
        if (explosion != null) {
            if (explosive) {
                managers.soundPlayer.play(
                    managers.assetsManager.getAssetByDefinition(
                        SoundDefinition.EXPLOSION_SMALL
                    )
                )
            }
            EntityBuilder.begin()
                .addParticleEffectComponent(
                    position,
                    gameSessionData.pools.particleEffectsPools.obtain(
                        explosion,
                    )
                ).finishAndAddToEngine()
            if (explosion.hasBlastRing) {
                addGroundBlast(position, blastRingTexture, 0.1F, 11F, 250, 0.03F)
            }
        } else {
            if (!explosive) {
                EntityBuilder.begin()
                    .addParticleEffectComponent(
                        position,
                        gameSessionData.pools.particleEffectsPools.obtain(
                            ParticleEffectDefinition.SMOKE_SMALL,
                        )
                    ).finishAndAddToEngine()
            }
        }
    }

    private fun addWaterSplash(position: Vector3) {
        EntityBuilder.begin()
            .addParticleEffectComponent(
                position,
                gameSessionData.pools.particleEffectsPools.obtain(
                    ParticleEffectDefinition.WATER_SPLASH
                )
            ).finishAndAddToEngine()
        managers.soundPlayer.play(
            waterSplashSounds.random(),
        )
        addGroundBlast(position, waterSplashFloorTexture, 0.5F, 1.01F, 2000, 0.01F)
    }

    override fun update(deltaTime: Float) {
        for (bullet in bulletEntities) {
            val bulletComponent = ComponentsMapper.bullet.get(bullet)
            if (bulletComponent.createdTime + 3000L > TimeUtils.millis()) {
                if (bulletComponent.behavior == BulletBehavior.CURVE) {
                    val physicsComponent = ComponentsMapper.physics.get(bullet)
                    val rotation = physicsComponent.rigidBody.worldTransform.getRotation(auxQuat)
                    val curveRotationStepSize = CURVE_ROTATION_STEP * deltaTime
                    if (rotation.roll > -90F) {
                        val worldTransform = physicsComponent.rigidBody.worldTransform
                        physicsComponent.rigidBody.worldTransform =
                            auxMatrix.set(worldTransform).rotate(Vector3.Z, curveRotationStepSize)
                        val orientation =
                            physicsComponent.rigidBody.worldTransform.getRotation(auxQuat)
                        val localZ = auxVector1.set(0F, 0F, 1F)
                        orientation.transform(localZ)
                        physicsComponent.rigidBody.linearVelocity =
                            physicsComponent.rigidBody.linearVelocity.rotate(
                                localZ,
                                curveRotationStepSize
                            )
                    }
                }
            } else {
                destroyBullet(bullet)
            }
        }
    }

    private fun destroyBullet(entity: Entity) {
        engine.removeEntity(entity)
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }

    companion object {
        val auxBoundingBox = BoundingBox()
        private val auxMatrix = Matrix4()
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxQuat = Quaternion()
        private const val CURVE_ROTATION_STEP = -90F
    }

}
