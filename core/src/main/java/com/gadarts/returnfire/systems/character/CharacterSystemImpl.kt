package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.assets.definitions.MapDefinition
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.StageComponent
import com.gadarts.returnfire.components.StageComponent.Companion.MAX_Y
import com.gadarts.returnfire.components.TurretComponent
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.physics.MotionState
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.components.physics.RigidBody
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.definitions.SimpleCharacterDefinition
import com.gadarts.returnfire.model.definitions.TurretCharacterDefinition
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.character.factories.OpponentCharacterFactory
import com.gadarts.returnfire.systems.character.react.*
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.data.GameSessionDataMap
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.render.RenderSystem
import com.gadarts.returnfire.utils.CharacterPhysicsInitializer
import kotlin.math.min

class CharacterSystemImpl(gamePlayManagers: GamePlayManagers) : CharacterSystem,
    GameEntitySystem(gamePlayManagers) {
    private val characterPhysicsInitializer = CharacterPhysicsInitializer()
    private val characterAmbSoundUpdater =
        CharacterAmbSoundUpdater(gamePlayManagers.soundPlayer, gamePlayManagers.ecs.engine)
    private val opponentCharacterFactory by lazy {
        OpponentCharacterFactory(
            gamePlayManagers.assetsManager,
            gameSessionData,
            gamePlayManagers.factories.gameModelInstanceFactory,
            gamePlayManagers.ecs.entityBuilder,
        )
    }
    private val relatedEntities by lazy { CharacterSystemRelatedEntities(engine) }
    private val flyingPartBoundingBox by lazy {
        this.gamePlayManagers.assetsManager.getCachedBoundingBox(
            ModelDefinition.FLYING_PART
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.CHARACTER_WEAPON_ENGAGED_PRIMARY to CharacterSystemOnCharacterWeaponShotPrimary(this),
        SystemEvents.CHARACTER_WEAPON_ENGAGED_SECONDARY to CharacterSystemOnCharacterWeaponShotSecondary(this),
        SystemEvents.PHYSICS_COLLISION to CharacterSystemOnPhysicsCollision(),
        SystemEvents.CHARACTER_REQUEST_BOARDING to CharacterSystemOnCharacterRequestBoarding(),
        SystemEvents.AMB_SOUND_COMPONENT_ADDED to CharacterSystemOnAmbSoundComponentAdded(this),
        SystemEvents.MAP_LOADED to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                val map = gamePlayManagers.assetsManager.getAssetByDefinition(MapDefinition.MAP_0)
                relatedEntities.baseEntities.forEach {
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
                    gamePlayManagers.ecs.engine.addEntity(opponent)
                    gamePlayManagers.dispatcher.dispatchMessage(
                        SystemEvents.OPPONENT_CHARACTER_CREATED.ordinal,
                        opponent
                    )
                }
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

    override fun initialize(gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        super.initialize(gameSessionData, gamePlayManagers)
        engine.addEntityListener(object : EntityListener {
            override fun entityAdded(entity: Entity) {
                playAmbSound(entity, gamePlayManagers)
            }

            override fun entityRemoved(entity: Entity) {
                if (ComponentsMapper.player.has(entity)) {
                    gameSessionData.gamePlayData.player = null
                }
            }

        })
    }

    override fun resume(delta: Long) {

    }

    override fun update(deltaTime: Float) {
        updateCharacters(deltaTime)
        updateTurrets()
        characterAmbSoundUpdater.update(deltaTime)
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

    override fun dispose() {
        opponentCharacterFactory.dispose()
    }


    override fun playAmbSound(entity: Entity, gamePlayManagers: GamePlayManagers) {
        if (!GameDebugSettings.DISABLE_AMB_SOUNDS && ComponentsMapper.ambSound.has(entity)) {
            val ambSoundComponent = ComponentsMapper.ambSound.get(entity)
            if (ambSoundComponent.soundId == -1L) {
                val id = gamePlayManagers.soundPlayer.loopSound(ambSoundComponent.sound)
                ambSoundComponent.soundId = id
            }
        }
    }


    private fun updateTurrets() {
        for (turret in relatedEntities.turretEntities) {
            val turretComponent = ComponentsMapper.turret.get(turret)
            val base = turretComponent.base
            if (turretComponent.followBase && ComponentsMapper.modelInstance.has(base)) {
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
        for (character in relatedEntities.charactersEntities) {
            val hasAmbSound = ComponentsMapper.ambSound.has(character)
            val modelInstanceComponent = ComponentsMapper.modelInstance.get(character)
            val characterTransform =
                modelInstanceComponent.gameModelInstance.modelInstance.transform
            val characterComponent = ComponentsMapper.character.get(character)
            val definition = characterComponent.definition
            val isApache = definition == SimpleCharacterDefinition.APACHE
            if (isApache && ComponentsMapper.childDecal.has(character)) {
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
                    if (stageTransform.getTranslation(auxVector1).y < MAX_Y) {
                        takeStepForElevatorWithCharacter(stageTransform, deltaTime, character)
                    } else {
                        val animationDone = updateBoardingAnimation(deltaTime, character)
                        if (animationDone && boardingComponent.isOffboarding()) {
                            boardingDone(character)
                        }
                    }
                } else {
                    val boardingAnimation = boardingComponent.boardingAnimation
                    val isAlreadyDone = boardingAnimation?.isDone() != false
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
                            takeStepForElevatorWithCharacter(stageTransform, -deltaTime, character)
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
                        ).y <= GameSessionDataMap.DROWNING_HEIGHT / 3
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
                                gamePlayManagers.ecs.entityBuilder.begin().addParticleEffectComponent(
                                    position = position,
                                    pool = gameSessionData.pools.particleEffectsPools.obtain(
                                        ParticleEffectDefinition.SMOKE_UP_LOOP
                                    ),
                                    followRelativePosition = definition.getSmokeEmissionRelativePosition(
                                        auxVector2
                                    )
                                ).finishAndAddToEngine()
                            ComponentsMapper.particleEffect.get(smoke).followEntity = character
                            characterComponent.smokeEmission = smoke
                        }
                    }
                } else if (characterComponent.deathSequenceNextExplosion < TimeUtils.millis()) {
                    characterComponent.incrementDeathSequence()
                    if (characterComponent.deathSequenceDuration <= 0) {
                        characterComponent.dead = true
                        if (hasAmbSound) {
                            val ambSoundComponent = ComponentsMapper.ambSound.get(character)
                            ambSoundComponent.sound.stop(ambSoundComponent.soundId)
                        }
                        addFlyingParts(character)
                        for (i in 0 until MathUtils.random(3, 4)) {
                            addExplosion(character)
                        }
                        val isTank = definition == TurretCharacterDefinition.TANK
                        var planeCrashSoundId = -1L
                        if (isApache) {
                            planeCrashSoundId = gamePlayManagers.soundPlayer.play(
                                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.PLANE_CRASH),
                                modelInstanceComponent.gameModelInstance.modelInstance.transform.getTranslation(
                                    auxVector1
                                )
                            )
                        }
                        if (isApache || isTank) {
                            if (!ComponentsMapper.player.has(character)) {
                                if (MathUtils.random() >= 0.5F) {
                                    turnCharacterToCorpse(character, planeCrashSoundId)
                                } else {
                                    gibCharacter(character, planeCrashSoundId)
                                }
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

    private fun gibCharacter(character: Entity, planeCrashSoundId: Long) {
        val position =
            ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val gameModelInstanceBack =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(ModelDefinition.APACHE_DEAD_BACK)
        val gameModelInstanceFront =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(ModelDefinition.APACHE_DEAD_FRONT)
        val assetsManager = gamePlayManagers.assetsManager
        val frontBoundingBox = assetsManager.getCachedBoundingBox(ModelDefinition.APACHE_DEAD_FRONT)
        val backBoundingBox = assetsManager.getCachedBoundingBox(ModelDefinition.APACHE_DEAD_BACK)
        val backShape = btCompoundShape()
        val btBackShapeMain = btBoxShape(Vector3(0.2F, 0.075F, 0.1F))
        val btBackShapeHorTail = btBoxShape(Vector3(0.05F, 0.05F, 0.135F))
        val btBackShapeVertTail = btBoxShape(Vector3(0.06F, 0.1F, 0.12F))
        backShape.addChildShape(Matrix4(), btBackShapeMain)
        backShape.addChildShape(Matrix4().translate(-0.43F, 0F, 0.12F), btBackShapeHorTail)
        backShape.addChildShape(Matrix4().translate(-0.47F, 0F, 0.08F), btBackShapeHorTail)
        backShape.addChildShape(Matrix4().translate(-0.43F, 0.1F, 0.12F), btBackShapeVertTail)
        val frontShape = btCompoundShape()
        val btFrontShapeMain = btBoxShape(Vector3(0.295F, 0.1F, 0.125F))
        val btFrontShapeWings = btBoxShape(Vector3(0.08F, 0.25F, 0.025F))
        frontShape.addChildShape(Matrix4(), btFrontShapeMain)
        frontShape.addChildShape(Matrix4().translate(-0.14F, 0F, 0.2F), btFrontShapeWings)
        addCharacterGiblet(gameModelInstanceBack, position, backBoundingBox, backShape, planeCrashSoundId)
        addCharacterGiblet(gameModelInstanceFront, position, frontBoundingBox, frontShape, planeCrashSoundId)
        engine.removeEntity(character)
    }

    private fun addCharacterGiblet(
        gameModelInstanceBack: GameModelInstance,
        position: Vector3,
        boundingBox: BoundingBox,
        btBoxShape: btCollisionShape,
        planeCrashSoundId: Long
    ) {
        val assetsManager = gamePlayManagers.assetsManager
        val part = gamePlayManagers.ecs.entityBuilder.begin().addModelInstanceComponent(
            model = gameModelInstanceBack,
            position = position,
            boundingBox = boundingBox,
            texture = assetsManager.getTexture("apache_texture_dead_green")
        ).addParticleEffectComponent(
            position = position,
            pool = gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.FIRE_LOOP),
            ttlInSeconds = MathUtils.random(20, 25),
            ttlForComponentOnly = true
        ).addPhysicsComponent(
            btBoxShape,
            CollisionFlags.CF_CHARACTER_OBJECT,
            gameModelInstanceBack.modelInstance.transform,
            0.4F,
            8F
        ).addCrashSoundEmitterComponent(
            assetsManager.getAssetByDefinition(SoundDefinition.PLANE_CRASH),
            planeCrashSoundId
        ).finishAndAddToEngine()
        val rigidBody = ComponentsMapper.physics.get(part).rigidBody
        pushRigidBodyRandomly(rigidBody, 13F)
    }

    private fun turnCharacterToCorpse(
        character: Entity,
        planeCrashSoundId: Long
    ) {
        val assetsManager = gamePlayManagers.assetsManager
        val deadGameModelInstance =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(
                ModelDefinition.APACHE_DEAD
            )
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(character)
        modelInstanceComponent.init(
            deadGameModelInstance,
            modelInstanceComponent.gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            ),
            assetsManager.getCachedBoundingBox(ModelDefinition.APACHE_DEAD),
            0F,
            false,
            assetsManager.getTexture("apache_texture_dead_green")
        )
        val rigidBody = ComponentsMapper.physics.get(character).rigidBody
        rigidBody.gravity = auxVector1.set(PhysicsComponent.worldGravity).scl(0.25F)
        ComponentsMapper.physics.get(character).rigidBody
        val motionState = rigidBody.motionState as MotionState
        val deadGameModelInstanceTransform = deadGameModelInstance.modelInstance.transform
        deadGameModelInstanceTransform.set(auxMatrix.set(rigidBody.worldTransform))
        motionState.transformObject = deadGameModelInstanceTransform
        motionState.setWorldTransform(deadGameModelInstanceTransform)
        rigidBody.linearFactor = Vector3(1F, 1F, 1F)
        rigidBody.angularFactor = Vector3(1F, 1F, 1F)
        pushRigidBodyRandomly(rigidBody, 3F)
        rigidBody.applyTorqueImpulse(
            Vector3(
                MathUtils.random(),
                MathUtils.random(),
                MathUtils.random()
            ).scl(2F)
        )
        character.remove(ChildDecalComponent::class.java)
        gamePlayManagers.ecs.entityBuilder.addCrashSoundEmitterComponentToEntity(
            character,
            gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.PLANE_CRASH),
            planeCrashSoundId
        )
        gamePlayManagers.ecs.entityBuilder.addParticleEffectComponentToEntity(
            entity = character,
            pool = gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.FIRE_LOOP),
            ttlInSeconds = MathUtils.random(20, 25),
            ttlForComponentOnly = true
        )
        gamePlayManagers.dispatcher.dispatchMessage(
            SystemEvents.PARTICLE_EFFECTS_COMPONENTS_ADDED_MANUALLY.ordinal,
            character
        )
    }

    private fun pushRigidBodyRandomly(rigidBody: RigidBody, scale: Float) {
        rigidBody.applyCentralImpulse(
            Vector3(Vector3.X).rotate(
                Vector3.Y,
                MathUtils.random(0F, 360F)
            ).scl(scale)
        )
    }

    private fun addExplosion(character: Entity) {
        val entity =
            if (ComponentsMapper.turretBase.has(character)) ComponentsMapper.turretBase.get(
                character
            ).turret else character
        gamePlayManagers.factories.specialEffectsFactory.generateExplosion(entity, true)
    }

    private fun takeStepForElevatorWithCharacter(
        elevatorTransform: Matrix4,
        deltaTime: Float,
        character: Entity
    ) {
        val oldStagePosition = elevatorTransform.getTranslation(auxVector3)
        elevatorTransform.trn(0F, deltaTime * 2F, 0F)
        val newPosition = elevatorTransform.getTranslation(auxVector1)
        newPosition.y = min(MAX_Y, newPosition.y)
        elevatorTransform.setTranslation(newPosition)
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
        characterPhysicsInitializer.initialize(
            gamePlayManagers.ecs.entityBuilder,
            character
        )
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
        val flyingPart = gamePlayManagers.ecs.entityBuilder.begin()
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
                1F,
            )
            .addParticleEffectComponent(
                modelInstance.transform.getTranslation(auxVector1),
                gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_UP_LOOP),
                ttlInSeconds = MathUtils.random(20, 25)
            )
            .finishAndAddToEngine()
        ComponentsMapper.physics.get(flyingPart).rigidBody.setDamping(0.2F, 0.5F)
        makeFlyingPartFlyAway(flyingPart)
    }

    companion object {
        private val auxMatrix = Matrix4()
        private val auxQuat = Quaternion()
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        const val ROT_STEP = 1600F
    }

}
