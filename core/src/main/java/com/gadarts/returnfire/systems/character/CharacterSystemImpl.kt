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
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.*
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.model.SimpleCharacterDefinition
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
    private val stageEntity: Entity by lazy {
        engine!!.getEntitiesFor(
            Family.all(StageComponent::class.java).get()
        ).first()
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
        },
        SystemEvents.CHARACTER_BOARDING to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                ComponentsMapper.boarding.get(gameSessionData.player).boardingAnimation?.reset()
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
        updateCharacters(deltaTime)
        updateTurrets()
        for (entity in ambSoundEntities) {
            val ambSoundComponent = ComponentsMapper.ambSound.get(entity)
            val sound = ambSoundComponent.sound
            val pitchTarget = ambSoundComponent.pitchTarget
            val pitch = ambSoundComponent.pitch
            if (!MathUtils.isEqual(pitchTarget, pitch, 0.1F)) {
                val calculatePitchStep = calculateNewPitch(pitchTarget, pitch, deltaTime)
                ambSoundComponent.pitch = calculatePitchStep
                sound.setPitch(ambSoundComponent.soundId, calculatePitchStep)
            }
        }
    }

    private fun calculateNewPitch(pitchTarget: Float, pitch: Float, deltaTime: Float): Float {
        val stepSize = PITCH_STEP_SIZE * deltaTime * 60F * (if (pitch < pitchTarget) 1F else -1F)
        return pitch + stepSize
    }

    private fun updateTurrets() {
        for (turret in turretEntities) {
            val turretComponent = ComponentsMapper.turret.get(turret)
            if (turretComponent.followBase) {
                val base = turretComponent.base
                val baseTransform = ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform
                baseTransform.getTranslation(auxVector1)
                val turretTransform =
                    ComponentsMapper.modelInstance.get(turret).gameModelInstance.modelInstance.transform
                turretTransform.setToTranslation(auxVector1).translate(auxVector2.set(0F, 0.2F, 0F))
                applyTurretOffsetFromBase(turretComponent, turretTransform)
                turretTransform.rotate(baseTransform.getRotation(auxQuat.idt()))
                turretTransform.rotate(Vector3.Y, turretComponent.turretRelativeRotation)
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

    private val explosionMedGameParticleEffectPool by lazy {
        gameSessionData.pools.particleEffectsPools.obtain(
            ParticleEffectDefinition.EXPLOSION_MED
        )
    }

    private fun updateCharacters(deltaTime: Float) {
        for (character in charactersEntities) {
            val characterComponent = ComponentsMapper.character.get(character)
            val characterTransform =
                ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
            if (characterComponent.definition == SimpleCharacterDefinition.APACHE) {
                val child = ComponentsMapper.childDecal.get(character).decals[0]
                child.rotationStep.setAngleDeg(child.rotationStep.angleDeg() + ROT_STEP * deltaTime)
                child.decal.rotation = characterTransform.getRotation(auxQuat)
                child.decal.rotateX(90F)
                child.decal.rotateZ(child.rotationStep.angleDeg())
            }
            val hp = characterComponent.hp
            if (ComponentsMapper.boarding.has(character) && ComponentsMapper.boarding.get(
                    character
                ).isBoarding()
            ) {
                val boardingComponent = ComponentsMapper.boarding.get(character)
                val stageTransform =
                    ComponentsMapper.modelInstance.get(stageEntity).gameModelInstance.modelInstance.transform
                if (boardingComponent.isOffboarding()
                ) {
                    if (stageTransform.getTranslation(auxVector1).y < -1F) {
                        takeStepForStageWithCharacter(stageTransform, deltaTime, character)
                    } else {
                        val animationDone = updateBoardingAnimation(deltaTime, character)
                        if (animationDone && boardingComponent.isOffboarding()) {
                            boardingDone(character)
                        }
                    }
                } else {
                    val boardingAnimation = ComponentsMapper.boarding.get(character).boardingAnimation
                    val isAlreadyDone = boardingAnimation?.isDone() ?: true
                    val animationDone = updateBoardingAnimation(deltaTime, character)
                    if (ComponentsMapper.physics.has(character)) {
                        val physicsComponent = ComponentsMapper.physics.get(character)
                        ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform =
                            Matrix4(physicsComponent.rigidBody.worldTransform)
                        managers.dispatcher.dispatchMessage(
                            SystemEvents.PHYSICS_COMPONENT_REMOVED_MANUALLY.ordinal,
                            physicsComponent
                        )
                        character.remove(PhysicsComponent::class.java)
                    }
                    if (animationDone) {
                        if (stageTransform.getTranslation(auxVector1).y <= StageComponent.BOTTOM_EDGE_Y) {
                            managers.screensManager.goToSelectionScreen()
                        } else {
                            takeStepForStageWithCharacter(stageTransform, -deltaTime, character)
                        }
                        if (!isAlreadyDone) {
                            managers.dispatcher.dispatchMessage(SystemEvents.CHARACTER_ONBOARDING_ANIMATION_DONE.ordinal)
                        }
                    }

                }
            } else if (!characterComponent.dead) {
                if (characterComponent.deathSequenceDuration <= 0) {
                    if (characterTransform.getTranslation(
                            auxVector1
                        ).y < -1F
                    ) {
                        managers.dispatcher.dispatchMessage(SystemEvents.CHARACTER_DIED.ordinal, character)
                    } else {
                        val smokeEmission = characterComponent.smokeEmission
                        if (hp <= 0 && characterComponent.deathSequenceDuration == 0) {
                            characterComponent.beginDeathSequence()
                        } else if (hp <= characterComponent.definition.getHP() / 2F && smokeEmission == null) {
                            val position =
                                characterTransform.getTranslation(
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
                } else if (characterComponent.deathSequenceNextExplosion < TimeUtils.millis()) {
                    characterComponent.incrementDeathSequence()
                    if (characterComponent.deathSequenceDuration <= 0) {
                        characterComponent.dead = true
                        managers.dispatcher.dispatchMessage(SystemEvents.CHARACTER_DIED.ordinal, character)
                    } else {
                        val entity =
                            if (ComponentsMapper.turretBase.has(character)) ComponentsMapper.turretBase.get(character).turret else character
                        EntityBuilder.begin().addParticleEffectComponent(
                            ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform.getTranslation(
                                auxVector1
                            ).add(
                                MathUtils.random(
                                    MathUtils.randomSign() + MED_EXPLOSION_DEATH_SEQUENCE_BIAS,
                                ), MathUtils.random(
                                    MED_EXPLOSION_DEATH_SEQUENCE_BIAS,
                                ), MathUtils.random(
                                    MathUtils.randomSign() + MED_EXPLOSION_DEATH_SEQUENCE_BIAS,
                                )
                            ), explosionMedGameParticleEffectPool
                        ).finishAndAddToEngine()
                        managers.soundPlayer.play(
                            managers.assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION),
                        )
                    }
                }
            }
        }
    }

    private fun takeStepForStageWithCharacter(
        stageTransform: Matrix4,
        deltaTime: Float,
        character: Entity
    ) {
        stageTransform.trn(0F, deltaTime, 0F)
        ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.trn(0F, deltaTime, 0F)
    }

    private fun updateBoardingAnimation(
        deltaTime: Float,
        character: Entity
    ): Boolean {
        val boardingComponent = ComponentsMapper.boarding.get(character)
        val boardingAnimation = boardingComponent.boardingAnimation ?: return true

        if (boardingAnimation.isDone()) return true

        return boardingAnimation.update(
            deltaTime,
            character,
            managers.soundPlayer,
            managers.assetsManager
        )
    }

    private fun boardingDone(character: Entity) {
        ComponentsMapper.boarding.get(character).boardingDone()
        managers.dispatcher.dispatchMessage(SystemEvents.CHARACTER_ONBOARDED.ordinal, character)
    }

    override fun dispose() {

    }

    companion object {
        private const val MED_EXPLOSION_DEATH_SEQUENCE_BIAS = 0.1F
        private val auxQuat = Quaternion()
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        const val ROT_STEP = 1600F
        private const val PITCH_STEP_SIZE = 0.05F
    }

}
