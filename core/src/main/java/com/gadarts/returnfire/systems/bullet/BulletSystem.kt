package com.gadarts.returnfire.systems.bullet

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseProxy
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.factories.SpecialEffectsFactory
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.definitions.CharacterDefinition
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.BulletCreationRequestEventData
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData


class BulletSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {

    private val bulletLogic = BulletLogic()

    private val blastRingTexture: Texture by lazy { this.gamePlayManagers.assetsManager.getTexture("blast_ring") }

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
                gamePlayManagers: GamePlayManagers
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
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                createBullet()
            }

        }
    )

    override fun initialize(gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        super.initialize(gameSessionData, gamePlayManagers)
        engine.addEntityListener(object : EntityListener {
            override fun entityAdded(entity: Entity) {

            }

            override fun entityRemoved(entity: Entity) {
                if (ComponentsMapper.bullet.has(entity)) {
                    gameSessionData.gamePlayData.pools.gameModelInstancePools[ComponentsMapper.modelInstance.get(
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

    override fun update(deltaTime: Float) {
        for (bullet in bulletEntities) {
            val rigidBody = ComponentsMapper.physics.get(bullet).rigidBody
            val position = rigidBody.worldTransform.getTranslation(auxVector1)
            val velocity: Vector3 = rigidBody.linearVelocity
            if (velocity.y > 0F && position.y > CharacterDefinition.FLYER_HEIGHT) {
                rigidBody.linearVelocity = auxVector2.set(velocity.x, 0f, velocity.z)
            }
            val destroyBullet = bulletLogic.update(bullet, deltaTime)
            if (destroyBullet) {
                destroyBullet(bullet)
            }
        }
    }

    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }

    private fun createBullet() {
        val spark = BulletCreationRequestEventData.armComponent.spark
        val parentModelInstanceComponent = ComponentsMapper.modelInstance.get(ComponentsMapper.spark.get(spark).parent)
        val parentTransform = parentModelInstanceComponent.gameModelInstance.modelInstance.transform
        val position = parentTransform.getTranslation(auxVector1)
        gamePlayManagers.soundPlayer.play(
            BulletCreationRequestEventData.armComponent.armProperties.shootingSound,
            position
        )
        position.add(BulletCreationRequestEventData.relativePosition)
        showSpark(position, parentTransform)
        val modelDefinition = BulletCreationRequestEventData.armComponent.armProperties.renderData.modelDefinition
        val gameModelInstance =
            gameSessionData.gamePlayData.pools.gameModelInstancePools[modelDefinition]!!.obtain()
        val noTarget = BulletCreationRequestEventData.target == null
        val aimSky = BulletCreationRequestEventData.aimSky
        val bulletBehavior =
            if (noTarget && !aimSky) BulletCreationRequestEventData.armComponent.bulletBehavior else BulletBehavior.REGULAR
        val bullet = createBulletEntity(gameModelInstance, position, bulletBehavior)
        applyPhysicsToBullet(
            bullet,
            gameModelInstance,
            BulletCreationRequestEventData.armComponent.armProperties,
        )
        addSmokeEmission(BulletCreationRequestEventData.armComponent.armProperties, gameModelInstance, position)
        addSparkParticleEffect(position, BulletCreationRequestEventData.armComponent)
    }

    private fun createBulletEntity(
        gameModelInstance: GameModelInstance,
        position: Vector3,
        bulletBehavior: BulletBehavior
    ): Entity {
        val entityBuilder = gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                gameModelInstance,
                position,
                BulletCreationRequestEventData.armComponent.armProperties.renderData.boundingBox
            )
            .addBulletComponent(
                bulletBehavior,
                BulletCreationRequestEventData.armComponent.armProperties.effectsData.explosion,
                BulletCreationRequestEventData.armComponent.armProperties.explosive,
                BulletCreationRequestEventData.friendly,
                BulletCreationRequestEventData.armComponent.armProperties.damage
            )
        addSmokeTrail(BulletCreationRequestEventData.armComponent, position)
        val bullet = entityBuilder.finishAndAddToEngine()
        return bullet
    }

    private fun addSparkParticleEffect(position: Vector3, arm: ArmComponent) {
        gamePlayManagers.ecs.entityBuilder.begin()
            .addParticleEffectComponent(
                position,
                arm.armProperties.effectsData.sparkParticleEffect
            )
            .finishAndAddToEngine()
    }

    private fun showSpark(
        position: Vector3?,
        parentTransform: Matrix4
    ) {
        val spark = BulletCreationRequestEventData.armComponent.spark
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
            gamePlayManagers.ecs.entityBuilder.begin()
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
        armProperties: ArmProperties,
    ) {
        val aimingTransform = BulletCreationRequestEventData.direction
        val transform = gameModelInstance.modelInstance.transform
        gamePlayManagers.ecs.entityBuilder.addPhysicsComponentPooledToEntity(
            bullet,
            armProperties.rigidBodyPool,
            CollisionFlags.CF_CHARACTER_OBJECT,
            transform,
        )
        transform.rotate(aimingTransform.getRotation(auxQuat))
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
        position: Vector3
    ) {
        val effectsData = arm.armProperties.effectsData
        if (effectsData.smokeTrail != null) {
            gamePlayManagers.ecs.entityBuilder.addParticleEffectComponent(
                position = position,
                pool = effectsData.smokeTrail,
            )
        }
    }

    private fun handleBulletCollision(entity0: Entity, entity1: Entity): Boolean {
        val isBullet = ComponentsMapper.bullet.has(entity0)
        if (isBullet) {
            val position =
                ComponentsMapper.modelInstance.get(entity0).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            val z = position.z.toInt()
            val x = position.x.toInt()
            val tilesEntities = gameSessionData.mapData.tilesEntities
            if (z >= 0 && z < tilesEntities.size && x >= 0 && x < tilesEntities[0].size) {
                val tileEntity = tilesEntities[z][x]
                val isGround = ComponentsMapper.ground.has(entity1)
                if (isGround && tileEntity != null && ComponentsMapper.ground.get(
                        tileEntity
                    ).isWater
                ) {
                    gamePlayManagers.factories.specialEffectsFactory.generateWaterSplash(
                        position.set(position.x, SpecialEffectsFactory.WATER_SPLASH_Y, position.z)
                    )
                } else {
                    addBulletExplosion(entity0, position)
                    if (isGround) {
                        addBulletHole(position, entity0)
                    }
                }
            }
            destroyBullet(entity0)
            if (ComponentsMapper.bullet.has(entity1)) {
                destroyBullet(entity1)
            }
            return true
        }
        return false
    }

    private fun addBulletHole(position: Vector3, bullet: Entity) {
        val bulletComponent = ComponentsMapper.bullet.get(bullet)
        val bulletHolesHandler = gamePlayManagers.bulletHolesHandler
        if (bulletComponent.explosive) {
            bulletHolesHandler.addBig(position)
        } else {
            bulletHolesHandler.addSmall(position)
        }
    }

    private fun addBulletExplosion(bullet: Entity, position: Vector3) {
        val bulletComponent = ComponentsMapper.bullet.get(bullet)
        val explosive = bulletComponent.explosive
        if (bulletComponent.explosion != null) {
            if (explosive) {
                gamePlayManagers.soundPlayer.play(
                    gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION_SMALL),
                    position
                )
            }
            gamePlayManagers.ecs.entityBuilder.begin()
                .addParticleEffectComponent(
                    position,
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(
                        bulletComponent.explosion!!,
                    )
                ).finishAndAddToEngine()
            addBlastRing(bulletComponent.explosion!!, position)
        } else {
            if (!explosive) {
                gamePlayManagers.ecs.entityBuilder.begin()
                    .addParticleEffectComponent(
                        position,
                        gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(
                            ParticleEffectDefinition.SMOKE_TINY,
                        )
                    ).finishAndAddToEngine()
            }
        }
    }

    private fun addBlastRing(
        explosion: ParticleEffectDefinition,
        position: Vector3
    ) {
        if (explosion.hasBlastRing) {
            gamePlayManagers.factories.specialEffectsFactory.generateGroundBlast(
                position,
                blastRingTexture,
                0.1F,
                11F,
                250,
                0.03F
            )
        }
    }

    private fun destroyBullet(entity: Entity) {
        engine.removeEntity(entity)
    }

    companion object {
        val auxBoundingBox = BoundingBox()
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxQuat = Quaternion()
    }

}
