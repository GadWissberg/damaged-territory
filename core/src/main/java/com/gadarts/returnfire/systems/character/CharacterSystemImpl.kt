package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.*
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

    private val charactersEntities: ImmutableArray<Entity> by lazy {
        engine!!.getEntitiesFor(
            Family.all(CharacterComponent::class.java).get()
        )
    }

    private val turretEntities: ImmutableArray<Entity> by lazy {
        engine!!.getEntitiesFor(
            Family.all(TurretComponent::class.java).get()
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

    private fun handleBulletCharacterCollision(first: Entity, second: Entity): Boolean {
        val isSecondCharacter = ComponentsMapper.character.has(second)
        val isSecondTurret = if (!isSecondCharacter) ComponentsMapper.turret.has(second) else false
        if (ComponentsMapper.bullet.has(first) && (isSecondCharacter || isSecondTurret)) {
            val damage = ComponentsMapper.bullet.get(first).damage
            if (isSecondCharacter) {
                ComponentsMapper.character.get(second).takeDamage(damage)
            } else {
                ComponentsMapper.character.get(ComponentsMapper.turret.get(second).base).takeDamage(damage)
            }
            EntityBuilder.begin()
                .addParticleEffectComponent(
                    ComponentsMapper.modelInstance.get(first).gameModelInstance.modelInstance.transform.getTranslation(
                        auxVector1
                    ), gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.RICOCHET)
                )
                .finishAndAddToEngine()
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
        updateCharacters()
        updateTurrets()
    }

    private fun updateTurrets() {
        for (turret in turretEntities) {
            val turretComponent = ComponentsMapper.turret.get(turret)
            if (turretComponent.followBase) {
                val base = turretComponent.base
                val baseTransform = ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform
                baseTransform.getTranslation(auxVector1)
                val turretTransform =
                    ComponentsMapper.modelInstance.get(turret).gameModelInstance.modelInstance.transform.setToTranslation(
                        auxVector1
                    ).translate(auxVector2.set(0F, 0.2F, 0F))
                applyTurretOffsetFromBase(turretComponent, turretTransform)
                turretTransform.rotate(baseTransform.getRotation(auxQuat.idt()))
            }
            val cannon = turretComponent.cannon
            if (cannon != null) {
                val turretTransform =
                    ComponentsMapper.modelInstance.get(turret).gameModelInstance.modelInstance.transform
                turretTransform.getTranslation(
                    auxVector1
                )
                ComponentsMapper.modelInstance.get(cannon).gameModelInstance.modelInstance.transform.setToTranslation(
                    auxVector1
                ).rotate(turretTransform.getRotation(auxQuat.idt())).translate(auxVector2.set(0.31F, 0F, 0F))
            }
        }
    }

    private fun applyTurretOffsetFromBase(
        turretComponent: TurretComponent,
        turretTransform: Matrix4,
    ) {
        if (turretComponent.baseOffsetApplied) {
            val offset = turretComponent.getBaseOffset(auxVector3)
            turretTransform.translate(offset)
            offset.lerp(Vector3.Zero, 0.05F)
            turretComponent.setBaseOffset(offset)
            if (offset.epsilonEquals(Vector3.Zero)) {
                turretComponent.baseOffsetApplied = false
            }
        }
    }

    private fun updateCharacters() {
        for (character in charactersEntities) {
            val characterComponent = ComponentsMapper.character.get(character)
            val hp = characterComponent.hp
            if (!characterComponent.dead) {
                val smokeEmission = characterComponent.smokeEmission
                if (hp <= 0) {
                    characterDies(characterComponent, character)
                } else if (hp <= characterComponent.definition.getHP() / 2F && smokeEmission == null) {
                    val position =
                        ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                            auxVector1
                        )
                    val smoke = EntityBuilder.begin().addParticleEffectComponent(
                        position = position,
                        pool = gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_LOOP),
                        parentRelativePosition = characterComponent.definition.getSmokeEmissionRelativePosition(
                            auxVector2
                        )
                    ).finishAndAddToEngine()
                    ComponentsMapper.particleEffect.get(smoke).parent = character
                    characterComponent.smokeEmission = smoke
                }
            }
        }
    }

    private fun characterDies(
        characterComponent: CharacterComponent,
        character: Entity
    ) {
        characterComponent.die()
        managers.dispatcher.dispatchMessage(SystemEvents.CHARACTER_DIED.ordinal, character)
        managers.soundPlayer.play(
            managers.assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION),
        )
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

    companion object {
        private val auxQuat = Quaternion()
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
    }

}
