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
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.definitions.MapDefinition
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.components.*
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.model.TurretCharacterDefinition
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.character.factories.OpponentCharacterFactory
import com.gadarts.returnfire.systems.character.react.CharacterSystemOnCharacterWeaponShotPrimary
import com.gadarts.returnfire.systems.character.react.CharacterSystemOnCharacterWeaponShotSecondary
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData
import com.gadarts.returnfire.systems.render.RenderSystem
import kotlin.math.min

class CharacterSystemImpl(gamePlayManagers: GamePlayManagers) : CharacterSystem,
    GameEntitySystem(gamePlayManagers) {

    private val opponentCharacterFactory by lazy {
        OpponentCharacterFactory(
            gamePlayManagers.assetsManager,
            gameSessionData,
            gamePlayManagers.factories.gameModelInstanceFactory,
            gamePlayManagers.entityBuilder,
        )
    }


    private val ambSoundEntities: ImmutableArray<Entity> by lazy {
        engine!!.getEntitiesFor(
            Family.all(AmbSoundComponent::class.java).get()
        )
    }

    private val baseEntities: ImmutableArray<Entity> by lazy {
        engine!!.getEntitiesFor(
            Family.all(BaseComponent::class.java).get()
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
    private val flyingPartBoundingBox by lazy {
        this.gamePlayManagers.assetsManager.getCachedBoundingBox(
            ModelDefinition.FLYING_PART
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.CHARACTER_WEAPON_ENGAGED_PRIMARY to CharacterSystemOnCharacterWeaponShotPrimary(
            this
        ),
        SystemEvents.CHARACTER_WEAPON_ENGAGED_SECONDARY to CharacterSystemOnCharacterWeaponShotSecondary(
            this
        ),
        SystemEvents.PHYSICS_COLLISION to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                handleBulletCharacterCollision(
                    PhysicsCollisionEventData.colObj0.userData as Entity,
                    PhysicsCollisionEventData.colObj1.userData as Entity
                ) || handleBulletCharacterCollision(
                    PhysicsCollisionEventData.colObj1.userData as Entity,
                    PhysicsCollisionEventData.colObj0.userData as Entity
                )
            }
        },
        SystemEvents.CHARACTER_REQUEST_BOARDING to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                val character = msg.extraInfo as Entity
                ComponentsMapper.boarding.get(character).onBoard()
                val boardingComponent =
                    ComponentsMapper.boarding.get(character)
                boardingComponent.boardingAnimation?.init(gameSessionData.mapData.stages[boardingComponent.color])
                gamePlayManagers.dispatcher.dispatchMessage(
                    SystemEvents.CHARACTER_BOARDING.ordinal,
                    character
                )
            }
        },
        SystemEvents.AMB_SOUND_COMPONENT_ADDED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                playAmbSound(msg.extraInfo as Entity, gamePlayManagers)
            }
        },
        SystemEvents.MAP_LOADED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                addOpponents()
            }
        },
        SystemEvents.MAP_SYSTEM_READY to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                val stageEntities =
                    engine.getEntitiesFor(Family.all(StageComponent::class.java).get())
                gameSessionData.mapData.stages =
                    stageEntities.associateBy(
                        { ComponentsMapper.base.get(ComponentsMapper.stage.get(it).base).color },
                        { it })
            }
        }
    )

    private fun handleBulletCharacterCollision(first: Entity, second: Entity): Boolean {
        val isSecondCharacter = ComponentsMapper.character.has(second)
        val isSecondTurret = if (!isSecondCharacter) ComponentsMapper.turret.has(second) else false
        if (ComponentsMapper.bullet.has(first) && (isSecondCharacter || isSecondTurret)) {
            val damage = ComponentsMapper.bullet.get(first).damage
            val damagedCharacter = if (isSecondCharacter) {
                ComponentsMapper.character.get(second)
            } else {
                ComponentsMapper.character.get(ComponentsMapper.turret.get(second).base)
            }
            damagedCharacter.takeDamage(damage)
            gamePlayManagers.entityBuilder.begin()
                .addParticleEffectComponent(
                    ComponentsMapper.modelInstance.get(first).gameModelInstance.modelInstance.transform.getTranslation(
                        auxVector1
                    ),
                    gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.RICOCHET)
                )
                .finishAndAddToEngine()
            return true
        }
        return false
    }

    override fun initialize(gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        super.initialize(gameSessionData, gamePlayManagers)
        engine.addEntityListener(object : EntityListener {
            override fun entityAdded(entity: Entity) {
                playAmbSound(entity, gamePlayManagers)
            }

            override fun entityRemoved(entity: Entity) {
            }

        })
    }

    private fun addOpponents() {
        val map = gamePlayManagers.assetsManager.getAssetByDefinition(MapDefinition.MAP_0)
        baseEntities.forEach {
            val base =
                map.placedElements.find { placedElement ->
                    placedElement.definition == ComponentsMapper.amb.get(
                        it
                    ).def
                }
            val characterColor = ComponentsMapper.base.get(it).color
            val opponent =
                opponentCharacterFactory.create(
                    base!!,
                    if (characterColor == CharacterColor.GREEN) SimpleCharacterDefinition.APACHE else gameSessionData.selected,
                    characterColor
                )
            engine.addEntity(opponent)
            gamePlayManagers.dispatcher.dispatchMessage(
                SystemEvents.OPPONENT_CHARACTER_CREATED.ordinal,
                opponent
            )
        }
    }

    private fun playAmbSound(entity: Entity, gamePlayManagers: GamePlayManagers) {
        if (ComponentsMapper.ambSound.has(entity)) {
            val ambSoundComponent = ComponentsMapper.ambSound.get(entity)
            if (ambSoundComponent.soundId == -1L) {
                val id = gamePlayManagers.soundPlayer.loopSound(ambSoundComponent.sound)
                ambSoundComponent.soundId = id
            }
        }
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
                val baseTransform =
                    ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform
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
                ).rotate(turretTransform.getRotation(auxQuat.idt()))
                    .translate(auxVector2.set(0.31F, 0F, 0F))
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


    private fun updateCharacters(deltaTime: Float) {
        for (character in charactersEntities) {
            val characterComponent = ComponentsMapper.character.get(character)
            val modelInstanceComponent = ComponentsMapper.modelInstance.get(character)
            val characterTransform =
                modelInstanceComponent.gameModelInstance.modelInstance.transform
            val definition = characterComponent.definition
            if (definition == SimpleCharacterDefinition.APACHE) {
                val child = ComponentsMapper.childDecal.get(character).decals[0]
                child.rotationStep.setAngleDeg(child.rotationStep.angleDeg() + ROT_STEP * deltaTime)
                child.decal.rotation = characterTransform.getRotation(auxQuat)
                child.decal.rotateX(90F)
                child.decal.rotateZ(child.rotationStep.angleDeg())
            }
            val hp = characterComponent.hp
            val boardingComponent = ComponentsMapper.boarding.get(
                character
            )
            if (boardingComponent != null && boardingComponent.isBoarding()) {
                val stageTransform =
                    ComponentsMapper.modelInstance.get(gameSessionData.mapData.stages[boardingComponent.color]).gameModelInstance.modelInstance.transform
                if (boardingComponent.isOffboarding()) {
                    if (stageTransform.getTranslation(auxVector1).y < -1F) {
                        takeStepForStageWithCharacter(stageTransform, deltaTime, character)
                    } else {
                        val animationDone = updateBoardingAnimation(deltaTime, character)
                        if (animationDone && boardingComponent.isOffboarding()) {
                            boardingDone(character)
                        }
                    }
                } else {
                    val boardingAnimation = boardingComponent.boardingAnimation
                    val isAlreadyDone = boardingAnimation?.isDone() ?: true
                    val animationDone = updateBoardingAnimation(deltaTime, character)
                    if (ComponentsMapper.physics.has(character)) {
                        val physicsComponent = ComponentsMapper.physics.get(character)
                        val matrix4 = Matrix4(physicsComponent.rigidBody.worldTransform)
                        modelInstanceComponent.gameModelInstance.modelInstance.transform =
                            matrix4
                        if (modelInstanceComponent.gameModelInstance.shadow != null) {
                            modelInstanceComponent.gameModelInstance.shadow!!.transform = matrix4
                        }
                        gamePlayManagers.dispatcher.dispatchMessage(
                            SystemEvents.PHYSICS_COMPONENT_REMOVED_MANUALLY.ordinal,
                            physicsComponent
                        )
                        character.remove(PhysicsComponent::class.java)
                    }
                    if (animationDone) {
                        if (stageTransform.getTranslation(auxVector1).y <= StageComponent.BOTTOM_EDGE_Y) {
                            gamePlayManagers.dispatcher.dispatchMessage(
                                SystemEvents.CHARACTER_ONBOARDING_FINISHED.ordinal,
                                character
                            )
                        } else {
                            takeStepForStageWithCharacter(stageTransform, -deltaTime, character)
                        }
                        if (!isAlreadyDone) {
                            gamePlayManagers.dispatcher.dispatchMessage(
                                SystemEvents.CHARACTER_ONBOARDING_ANIMATION_DONE.ordinal,
                                character
                            )
                        }
                    }
                }
            } else if (!characterComponent.dead) {
                if (characterComponent.deathSequenceDuration <= 0) {
                    if (characterTransform.getTranslation(
                            auxVector1
                        ).y < -1F
                    ) {
                        gamePlayManagers.dispatcher.dispatchMessage(
                            SystemEvents.CHARACTER_DIED.ordinal,
                            character
                        )
                    } else {
                        val smokeEmission = characterComponent.smokeEmission
                        if (hp <= 0 && characterComponent.deathSequenceDuration == 0) {
                            characterComponent.beginDeathSequence()
                        } else if (hp <= definition.getHP() / 2F && smokeEmission == null) {
                            val position =
                                characterTransform.getTranslation(
                                    auxVector1
                                )
                            val smoke =
                                gamePlayManagers.entityBuilder.begin().addParticleEffectComponent(
                                    position = position,
                                    pool = gameSessionData.pools.particleEffectsPools.obtain(
                                        ParticleEffectDefinition.SMOKE_UP_LOOP
                                    ),
                                    parentRelativePosition = definition.getSmokeEmissionRelativePosition(
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
                        if (ComponentsMapper.ambSound.has(character)) {
                            val ambSoundComponent = ComponentsMapper.ambSound.get(character)
                            ambSoundComponent.sound.stop(ambSoundComponent.soundId)
                        }
                        addFlyingParts(character)
                        for (i in 0 until MathUtils.random(3, 4)) {
                            addExplosion(character)
                        }
                        if (definition == SimpleCharacterDefinition.APACHE || definition == TurretCharacterDefinition.TANK) {
                            if (!ComponentsMapper.player.has(character)) {
                                engine.removeEntity(character)
                            }
                        }
                        gamePlayManagers.dispatcher.dispatchMessage(
                            SystemEvents.CHARACTER_DIED.ordinal,
                            character
                        )
                    } else {
                        addExplosion(character)
                    }
                }
            }
        }
    }

    private fun addExplosion(character: Entity) {
        val entity =
            if (ComponentsMapper.turretBase.has(character)) ComponentsMapper.turretBase.get(
                character
            ).turret else character
        gamePlayManagers.factories.specialEffectsFactory.generateExplosion(entity)
    }

    private fun takeStepForStageWithCharacter(
        stageTransform: Matrix4,
        deltaTime: Float,
        character: Entity
    ) {
        val oldStagePosition = stageTransform.getTranslation(auxVector3)
        stageTransform.trn(0F, deltaTime, 0F)
        val newPosition = stageTransform.getTranslation(auxVector1)
        newPosition.y = min(-1F, newPosition.y)
        stageTransform.setTranslation(newPosition)
        ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.trn(
            0F,
            newPosition.y - oldStagePosition.y,
            0F
        )
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
            gamePlayManagers.soundPlayer,
            gamePlayManagers.assetsManager
        )
    }

    private fun boardingDone(character: Entity) {
        ComponentsMapper.boarding.get(character).boardingDone()
        gamePlayManagers.dispatcher.dispatchMessage(
            SystemEvents.CHARACTER_OFF_BOARDED.ordinal,
            character
        )
    }

    private fun addFlyingParts(character: Entity) {
        val transform = if (ComponentsMapper.turretBase.has(character)) {
            val turretModelInstanceComponent =
                ComponentsMapper.modelInstance.get(ComponentsMapper.turretBase.get(character).turret)
            turretModelInstanceComponent.gameModelInstance.modelInstance.transform
        } else {
            ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
        }
        val numberOfFlyingParts = MathUtils.random(2, 4)
        transform.getTranslation(auxVector2)
        for (i in 0 until numberOfFlyingParts) {
            addFlyingPart(auxVector2)
        }
    }

    private fun makeFlyingPartFlyAway(flyingPart: Entity) {
        val rigidBody = ComponentsMapper.physics.get(flyingPart).rigidBody
        rigidBody.applyCentralImpulse(
            createRandomDirectionUpwards()
        )
        rigidBody.applyTorque(createRandomDirectionUpwards())
    }

    private fun createRandomDirectionUpwards(): Vector3 {
        return auxVector1.set(1F, 0F, 0F).mul(
            auxQuat.idt()
                .setEulerAngles(
                    MathUtils.random(360F),
                    MathUtils.random(360F),
                    MathUtils.random(45F, 135F)
                )
        ).scl(MathUtils.random(3F, 5F))
    }

    private fun addFlyingPart(
        @Suppress("SameParameterValue") position: Vector3,
    ) {
        val modelInstance = ModelInstance(
            gamePlayManagers.assetsManager.getAssetByDefinition(ModelDefinition.FLYING_PART)
        )
        val gameModelInstance = GameModelInstance(modelInstance, ModelDefinition.FLYING_PART)
        val flyingPart = gamePlayManagers.entityBuilder.begin()
            .addModelInstanceComponent(
                gameModelInstance,
                position,
                flyingPartBoundingBox
            )
            .addPhysicsComponent(
                btBoxShape(
                    flyingPartBoundingBox.getDimensions(
                        auxVector1
                    ).scl(0.4F)
                ),
                CollisionFlags.CF_CHARACTER_OBJECT,
                modelInstance.transform,
                true,
            )
            .addParticleEffectComponent(
                modelInstance.transform.getTranslation(auxVector1),
                gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_UP_LOOP),
                thisEntityAsParent = true,
                ttlInSeconds = MathUtils.random(20, 25)
            )
            .finishAndAddToEngine()
        ComponentsMapper.physics.get(flyingPart).rigidBody.setDamping(0.2F, 0.5F)
        makeFlyingPartFlyAway(flyingPart)
    }

    override fun dispose() {
        opponentCharacterFactory.dispose()
    }

    companion object {
        private val auxQuat = Quaternion()
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        const val ROT_STEP = 1600F
        private const val PITCH_STEP_SIZE = 0.05F
    }

}
