package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBroadphaseProxy.CollisionFilterGroups
import com.badlogic.gdx.physics.bullet.collision.btSphereShape
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.AmbSoundComponent
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.character.react.CharacterSystemOnPlayerWeaponShotPrimary
import com.gadarts.returnfire.systems.character.react.CharacterSystemOnPlayerWeaponShotSecondary
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
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
    )

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
            GeneralUtils.calculateVolumeAccordingToPosition(entity, gameSessionData.gameSessionDataRender.camera)
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
        speed: Float,
        relativePosition: Vector3,
        radius: Float,
        explosion: ParticleEffect?,
        spark: Entity
    ) {
        val sparkComponent = ComponentsMapper.spark.get(spark)
        val sparkModelInstanceComponent = ComponentsMapper.modelInstance.get(spark)
        sparkModelInstanceComponent.gameModelInstance.modelInstance.transform.setTranslation(
            sparkComponent.relativePositionCalculator.calculate(
                sparkComponent.parent,
                auxVector1
            ).add(
                ComponentsMapper.modelInstance.get(sparkComponent.parent).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector2
                )
            )
        )
        sparkModelInstanceComponent.hideAt = TimeUtils.millis() + 500L
        sparkModelInstanceComponent.hidden = false
        val transform =
            ComponentsMapper.modelInstance.get(gameSessionData.player).gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector1)
        position.add(relativePosition)
        val gameModelInstance = PlayerWeaponShotEventData.pool.obtain()
        val bullet = EntityBuilder.begin()
            .addModelInstanceComponent(gameModelInstance, position, false)
            .addBulletComponent(
                PlayerWeaponShotEventData.pool,
                PlayerWeaponShotEventData.behavior,
                explosion
            )
            .finishAndAddToEngine()
        applyPhysicsToBullet(radius, bullet, gameModelInstance, transform, speed)
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
            gameModelInstance.modelInstance.transform.getRotation(auxQuat).transform(auxVector1.set(1F, 0F, 0F))
                .scl(speed)
        physicsComponent.rigidBody.worldTransform = gameModelInstance.modelInstance.transform
        physicsComponent.rigidBody.gravity = Vector3.Zero
        physicsComponent.rigidBody.contactCallbackFilter =
            CollisionFilterGroups.AllFilter
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxQuat = Quaternion()
    }

}
