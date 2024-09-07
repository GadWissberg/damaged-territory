package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.components.AmbSoundComponent
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.character.react.CharacterSystemOnCharacterWeaponShotPrimary
import com.gadarts.returnfire.systems.character.react.CharacterSystemOnCharacterWeaponShotSecondary
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData
import com.gadarts.returnfire.systems.render.RenderSystem

class CharacterSystemImpl : CharacterSystem, GameEntitySystem() {


    private val ambSoundEntities: ImmutableArray<Entity> by lazy {
        engine!!.getEntitiesFor(
            Family.all(AmbSoundComponent::class.java).get()
        )
    }
    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.CHARACTER_WEAPON_ENGAGED_PRIMARY to CharacterSystemOnCharacterWeaponShotPrimary(this),
        SystemEvents.CHARACTER_WEAPON_ENGAGED_SECONDARY to CharacterSystemOnCharacterWeaponShotSecondary(this),
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
        friendly: Boolean
    ) {
    }

    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxQuat = Quaternion()
    }

}
