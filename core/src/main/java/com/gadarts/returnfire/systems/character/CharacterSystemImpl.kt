package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseProxy.CollisionFilterGroups
import com.badlogic.gdx.physics.bullet.collision.btSphereShape
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.components.AmbSoundComponent
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.character.react.CharacterSystemOnPlayerWeaponShotPrimary
import com.gadarts.returnfire.systems.character.react.CharacterSystemOnPlayerWeaponShotSecondary
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData
import com.gadarts.returnfire.systems.events.data.PlayerWeaponShotEventData
import com.gadarts.returnfire.systems.render.RenderSystem

class CharacterSystemImpl : CharacterSystem, GameEntitySystem() {


    private val ambSoundEntities: ImmutableArray<Entity> by lazy {
        engine!!.getEntitiesFor(
            Family.all(AmbSoundComponent::class.java).get()
        )
    }
    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.PLAYER_WEAPON_SHOT_PRIMARY to CharacterSystemOnPlayerWeaponShotPrimary(this),
        SystemEvents.PLAYER_WEAPON_SHOT_SECONDARY to CharacterSystemOnPlayerWeaponShotSecondary(this),
        SystemEvents.PHYSICS_COLLISION to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                handleBulletCharacterCollision(
                    PhysicsCollisionEventData.colObj0.userData as Entity,
                    PhysicsCollisionEventData.colObj1.userData as Entity
                ) || handleBulletCharacterCollision(
                    PhysicsCollisionEventData.colObj1.userData as Entity,
                    PhysicsCollisionEventData.colObj0.userData as Entity
                )
            }
        }

    )

    private fun handleBulletCharacterCollision(entity0: Entity, entity1: Entity): Boolean {
        if (ComponentsMapper.bullet.has(entity0) && ComponentsMapper.enemy.has(entity1)) {
            EntityBuilder.begin().addParticleEffectComponent(
                ComponentsMapper.modelInstance.get(entity0).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                ), gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.RICOCHET)
            ).finishAndAddToEngine()
            return true
        }
        return false
    }

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        engine.addEntityListener(object : EntityListener {
            override fun entityAdded(entity: Entity?) {
                if (ComponentsMapper.ambSound.has(entity)) {
                    val ambSoundComponent = ComponentsMapper.ambSound.get(entity)
                    if (ambSoundComponent.soundId == -1L) {
                        val id = managers.soundPlayer.loopSound(ambSoundComponent.sound)
                        ambSoundComponent.soundId = id
                    }
                }
            }

            override fun entityRemoved(entity: Entity?) {
            }

        })
    }

    override fun positionSpark(
        arm: ArmComponent,
        modelInstance: ModelInstance,
        relativePosition: Vector3
    ): Entity {
        val spark = arm.spark
        ComponentsMapper.modelInstance.get(spark).gameModelInstance.modelInstance.transform.setTranslation(
            modelInstance.transform.getTranslation(RenderSystem.auxVector3_1).add(relativePosition)
        )
        return spark
    }

    override fun resume(delta: Long) {

    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        update3dSound()
    }

    private fun update3dSound() {
        for (entity in ambSoundEntities) {
            updateEntity3dSound(entity)
        }
    }

    private fun updateEntity3dSound(entity: Entity) {
        val distance =
            GeneralUtils.calculateVolumeAccordingToPosition(entity, gameSessionData.renderData.camera)
        val ambSoundComponent = ComponentsMapper.ambSound.get(entity)
        ambSoundComponent.sound.setVolume(ambSoundComponent.soundId, distance)
        if (distance <= 0F) {
            stopSoundOfEntity(ambSoundComponent)
        }
    }

    private fun stopSoundOfEntity(ambSoundComponent: AmbSoundComponent) {
        ambSoundComponent.sound.stop(ambSoundComponent.soundId)
        ambSoundComponent.soundId = -1
    }

    override fun dispose() {

    }

    override fun createBullet(
        arm: ArmComponent,
        relativePosition: Vector3,
    ) {
        val armProperties = arm.armProperties
        val spark = arm.spark
        val parentTransform =
            ComponentsMapper.modelInstance.get(ComponentsMapper.spark.get(spark).parent).gameModelInstance.modelInstance.transform
        val position = parentTransform.getTranslation(auxVector1)
        position.add(relativePosition)
        showSpark(spark, position, parentTransform)
        val gameModelInstance =
            gameSessionData.pools.gameModelInstancePools[armProperties.modelDefinition]!!.obtain()
        val entityBuilder = EntityBuilder.begin()
            .addModelInstanceComponent(gameModelInstance, position, false)
            .addBulletComponent(
                PlayerWeaponShotEventData.behavior,
                armProperties.explosion,
                armProperties.explosive
            )
        addSmokeTrail(arm, entityBuilder, position)
        val bullet = entityBuilder.finishAndAddToEngine()
        applyPhysicsToBullet(
            armProperties.radius,
            bullet,
            gameModelInstance,
            parentTransform,
            armProperties.speed
        )
        addSmokeEmission(armProperties, gameModelInstance, position)
        addSparkParticleEffect(position, arm)
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

    private fun addSparkParticleEffect(position: Vector3, arm: ArmComponent) {
        EntityBuilder.begin()
            .addParticleEffectComponent(
                position,
                arm.armProperties.sparkParticleEffect
            )
            .finishAndAddToEngine()
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

    private fun showSpark(
        spark: Entity,
        position: Vector3?,
        parentTransform: Matrix4
    ) {
        val sparkModelInstanceComponent = ComponentsMapper.modelInstance.get(spark)
        val sparkTransform = sparkModelInstanceComponent.gameModelInstance.modelInstance.transform
        sparkTransform.setToTranslation(position).rotate(parentTransform.getRotation(auxQuat)).rotate(Vector3.Z, -30F)
        sparkTransform.rotate(
            Vector3.X, MathUtils.random(360F)
        )
        sparkModelInstanceComponent.hideAt = TimeUtils.millis() + 50L
        sparkModelInstanceComponent.hidden = false
    }

    private fun applyPhysicsToBullet(
        radius: Float,
        bullet: Entity,
        gameModelInstance: GameModelInstance,
        transform: Matrix4,
        speed: Float
    ) {
        val shape = btSphereShape(radius)
        EntityBuilder.addPhysicsComponent(
            shape,
            bullet,
            managers.dispatcher,
            gameModelInstance.modelInstance.transform,
            10F
        )
        gameModelInstance.modelInstance.transform.rotate(transform.getRotation(auxQuat)).rotate(
            Vector3.Z,
            if (PlayerWeaponShotEventData.behavior == BulletBehavior.REGULAR) -45F else 0F
        )
        val physicsComponent = ComponentsMapper.physics.get(bullet)
        physicsComponent.rigidBody.linearVelocity =
            gameModelInstance.modelInstance.transform.getRotation(auxQuat).transform(auxVector2.set(1F, 0F, 0F))
                .scl(speed)
        physicsComponent.rigidBody.worldTransform = gameModelInstance.modelInstance.transform
        physicsComponent.rigidBody.gravity = Vector3.Zero
        physicsComponent.rigidBody.contactCallbackFilter =
            CollisionFilterGroups.AllFilter
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxQuat = Quaternion()
    }

}
