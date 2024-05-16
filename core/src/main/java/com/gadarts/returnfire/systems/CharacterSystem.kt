package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.Services
import com.gadarts.returnfire.components.AmbSoundComponent
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.BulletComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.render.RenderSystem

class CharacterSystem : GameEntitySystem() {

    private lateinit var bulletEntities: ImmutableArray<Entity>
    private lateinit var ambSoundEntities: ImmutableArray<Entity>

    override fun initialize(gameSessionData: GameSessionData, services: Services) {
        super.initialize(gameSessionData, services)
        ambSoundEntities = engine!!.getEntitiesFor(Family.all(AmbSoundComponent::class.java).get())
        engine.addEntityListener(object : EntityListener {
            override fun entityAdded(entity: Entity?) {
                if (ComponentsMapper.ambSound.has(entity)) {
                    val ambSoundComponent = ComponentsMapper.ambSound.get(entity)
                    if (ambSoundComponent.soundId == -1L) {
                        val id = services.soundPlayer.loopSound(ambSoundComponent.sound)
                        ambSoundComponent.soundId = id
                    }
                }
            }

            override fun entityRemoved(entity: Entity?) {
            }

        })
        bulletEntities = engine.getEntitiesFor(Family.all(BulletComponent::class.java).get())
    }

    override fun onSystemReady() {

    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.PLAYER_WEAPON_SHOT_PRIMARY to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, services: Services) {
                val arm = ComponentsMapper.primaryArm.get(gameSessionData.player)
                val relativePosition = arm.relativePos
                positionSpark(
                    arm, ComponentsMapper.modelInstance.get(gameSessionData.player).modelInstance,
                    relativePosition
                )
                val armProperties = arm.armProperties
                createBullet(
                    gameSessionData.player,
                    msg.extraInfo as ModelInstance,
                    armProperties.speed,
                    relativePosition
                )
                services.soundPlayer.playPositionalSound(
                    armProperties.shootingSound,
                    randomPitch = false,
                    gameSessionData.player,
                    this@CharacterSystem.gameSessionData.camera
                )
            }
        },
        SystemEvents.PLAYER_WEAPON_SHOT_SECONDARY to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, services: Services) {
                val arm = ComponentsMapper.secondaryArm.get(gameSessionData.player)
                val relativePosition = arm.relativePos
                positionSpark(
                    arm, ComponentsMapper.modelInstance.get(gameSessionData.player).modelInstance,
                    relativePosition
                )
                val armProperties = arm.armProperties
                createBullet(
                    gameSessionData.player,
                    msg.extraInfo as ModelInstance,
                    armProperties.speed,
                    relativePosition
                )
                services.soundPlayer.playPositionalSound(
                    armProperties.shootingSound,
                    randomPitch = false,
                    gameSessionData.player,
                    this@CharacterSystem.gameSessionData.camera
                )
            }
        },
    )

    override fun resume(delta: Long) {

    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        update3dSound()
        handleBullets(deltaTime)
    }


    private fun handleBullets(deltaTime: Float) {
        for (bullet in bulletEntities) {
            val transform = ComponentsMapper.modelInstance.get(bullet).modelInstance.transform
            takeStepForBullet(bullet, transform, deltaTime)
            val currentPosition = transform.getTranslation(auxVector1)
            val dst = ComponentsMapper.bullet.get(bullet).initialPosition.dst2(currentPosition)
            if (dst > BULLET_MAX_DISTANCE || currentPosition.y <= 0) {
                val pooledEngine = engine as PooledEngine
                pooledEngine.removeEntity(bullet)
            }
        }
    }

    private fun takeStepForBullet(
        bullet: Entity,
        transform: Matrix4,
        deltaTime: Float
    ) {
        val speed = ComponentsMapper.bullet.get(bullet).speed
        transform.trn(getDirectionOfModel(bullet).nor().scl(speed * deltaTime))
    }

    private fun update3dSound() {
        for (entity in ambSoundEntities) {
            updateEntity3dSound(entity)
        }
    }

    private fun updateEntity3dSound(entity: Entity) {
        val distance = GeneralUtils.calculateVolumeAccordingToPosition(entity, gameSessionData.camera)
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


    private fun createBullet(
        player: Entity,
        bulletModelInstance: ModelInstance,
        speed: Float,
        relativePosition: Vector3
    ) {
        val transform = ComponentsMapper.modelInstance.get(player).modelInstance.transform
        val position = transform.getTranslation(auxVector1)
        position.add(relativePosition)
        EntityBuilder.begin().addModelInstanceComponent(bulletModelInstance, position)
            .addBulletComponent(position, speed)
            .finishAndAddToEngine()
        tiltBullet(bulletModelInstance.transform, transform)
    }

    private fun tiltBullet(
        bulletTransform: Matrix4,
        transform: Matrix4
    ) {
        bulletTransform.rotate(transform.getRotation(auxQuat))
            .rotate(Vector3.X, MathUtils.random(-BULLET_TILT_BIAS, BULLET_TILT_BIAS))
            .rotate(Vector3.Y, MathUtils.random(-BULLET_TILT_BIAS, BULLET_TILT_BIAS))
            .rotate(Vector3.Z, MathUtils.random(-BULLET_TILT_BIAS, BULLET_TILT_BIAS))
    }

    private fun getDirectionOfModel(entity: Entity): Vector3 {
        val transform = ComponentsMapper.modelInstance.get(entity).modelInstance.transform
        auxVector2.set(1F, 0F, 0F).rot(transform)
        return auxVector2
    }


    private fun positionSpark(
        armComp: ArmComponent,
        modelInstance: ModelInstance,
        relativePosition: Vector3
    ): Decal {
        val decal = armComp.sparkDecal
        decal.position = modelInstance.transform.getTranslation(RenderSystem.auxVector3_1)
        decal.position.add(relativePosition)
        return decal
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxQuat = Quaternion()
        private const val BULLET_MAX_DISTANCE = 100F
        private const val BULLET_TILT_BIAS = 0.8F
    }

}
