package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.*
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.components.AmbSoundComponent
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.BulletComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.player.PlayerSystemEventsSubscriber
import com.gadarts.returnfire.systems.render.RenderSystem

class CharacterSystem : GameEntitySystem(), PlayerSystemEventsSubscriber {

    private lateinit var bulletEntities: ImmutableArray<Entity>
    private lateinit var ambSoundEntities: ImmutableArray<Entity>

    override fun initialize(am: GameAssetManager) {
        bulletEntities = engine.getEntitiesFor(Family.all(BulletComponent::class.java).get())
    }

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
        val distance = GeneralUtils.calculateVolumeAccordingToPosition(entity, commonData.camera)
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

    override fun addedToEngine(engine: Engine?) {
        super.addedToEngine(engine)
        ambSoundEntities = engine!!.getEntitiesFor(Family.all(AmbSoundComponent::class.java).get())
        engine.addEntityListener(object : EntityListener {
            override fun entityAdded(entity: Entity?) {
                if (ComponentsMapper.ambSound.has(entity)) {
                    val ambSoundComponent = ComponentsMapper.ambSound.get(entity)
                    if (ambSoundComponent.soundId == -1L) {
                        val id = soundPlayer.loopSound(ambSoundComponent.sound)
                        ambSoundComponent.soundId = id
                    }
                }
            }

            override fun entityRemoved(entity: Entity?) {
            }

        })
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

    override fun onPlayerWeaponShot(
        player: Entity,
        bulletModelInstance: ModelInstance,
        arm: ArmComponent,
    ) {
        val relativePosition = arm.relativePos
        positionSpark(
            arm, ComponentsMapper.modelInstance.get(player).modelInstance,
            relativePosition
        )
        val armProperties = arm.armProperties
        createBullet(player, bulletModelInstance, armProperties.speed, relativePosition)
        soundPlayer.playPositionalSound(
            armProperties.shootingSound,
            randomPitch = false,
            player,
            commonData.camera
        )
    }

    override fun onPlayerEnteredNewRegion(player: Entity) {

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
