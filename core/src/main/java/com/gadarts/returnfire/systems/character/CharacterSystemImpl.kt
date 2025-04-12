package com.gadarts.returnfire.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntityListener
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.CharacterComponent
import com.gadarts.returnfire.components.ChildModelInstanceComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.StageComponent
import com.gadarts.returnfire.components.StageComponent.Companion.MAX_Y
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.physics.MotionState
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.components.physics.RigidBody
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.CharacterType
import com.gadarts.returnfire.model.definitions.SimpleCharacterDefinition
import com.gadarts.returnfire.model.definitions.TurretCharacterDefinition
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.character.react.*
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.data.GameSessionDataMap
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.render.RenderSystem
import com.gadarts.returnfire.utils.CharacterPhysicsInitializer
import kotlin.math.abs

class CharacterSystemImpl(gamePlayManagers: GamePlayManagers) : CharacterSystem,
    GameEntitySystem(gamePlayManagers) {
    private val turretsHandler = TurretsHandler(gamePlayManagers.ecs.engine)
    private val characterPhysicsInitializer = CharacterPhysicsInitializer()
    private val characterAmbSoundHandler =
        CharacterAmbSoundHandler(gamePlayManagers.soundPlayer, gamePlayManagers.ecs.engine)
    private val charactersEntities: ImmutableArray<Entity> = gamePlayManagers.ecs.engine.getEntitiesFor(
        Family.all(CharacterComponent::class.java).get()
    )

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.CHARACTER_WEAPON_ENGAGED_PRIMARY to CharacterSystemOnCharacterWeaponShotPrimary(this),
        SystemEvents.CHARACTER_WEAPON_ENGAGED_SECONDARY to CharacterSystemOnCharacterWeaponShotSecondary(this),
        SystemEvents.PHYSICS_COLLISION to CharacterSystemOnPhysicsCollision(),
        SystemEvents.CHARACTER_REQUEST_BOARDING to CharacterSystemOnCharacterRequestBoarding(),
        SystemEvents.AMB_SOUND_COMPONENT_ADDED to CharacterSystemOnAmbSoundComponentAdded(this),
        SystemEvents.MAP_LOADED to CharacterSystemOnMapLoaded(gamePlayManagers.ecs.engine),
        SystemEvents.MAP_SYSTEM_READY to object : HandlerOnEvent {
            override fun react(
                msg: Telegram,
                gameSessionData: GameSessionData,
                gamePlayManagers: GamePlayManagers
            ) {
                val stageEntities =
                    engine.getEntitiesFor(Family.all(StageComponent::class.java).get())
                gameSessionData.mapData.hangars =
                    stageEntities.associateBy(
                        { ComponentsMapper.hangar.get(ComponentsMapper.stage.get(it).base).color },
                        { it })
            }
        },
        SystemEvents.DEATH_SEQUENCE_FINISHED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                if (!ComponentsMapper.character.has(msg.extraInfo as Entity)) return

                destroyCharacter(msg, gamePlayManagers)
            }
        })

    private fun destroyCharacter(
        msg: Telegram,
        gamePlayManagers: GamePlayManagers
    ) {
        val character = msg.extraInfo as Entity
        val characterComponent = ComponentsMapper.character.get(character)
        characterComponent.dead = true
        if (ComponentsMapper.ambSound.has(character)) {
            val ambSoundComponent = ComponentsMapper.ambSound.get(character)
            ambSoundComponent.sound.stop(ambSoundComponent.soundId)
        }
        gamePlayManagers.factories.specialEffectsFactory.generateFlyingParts(character)
        for (i in 0 until MathUtils.random(3, 4)) {
            gamePlayManagers.factories.specialEffectsFactory.generateExplosionForCharacter(character)
        }
        val isApache = characterComponent.definition == SimpleCharacterDefinition.APACHE
        var planeCrashSoundId = -1L
        if (isApache) {
            planeCrashSoundId = gamePlayManagers.soundPlayer.play(
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.PLANE_CRASH),
                ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            )
        }
        if (isApache || characterComponent.definition == TurretCharacterDefinition.TANK) {
            if (!ComponentsMapper.player.has(character)) {
                if (!characterComponent.definition.isGibable() || MathUtils.random() >= 0.5F) {
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
    }

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
        turretsHandler.update()
        characterAmbSoundHandler.update(deltaTime)
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


    private fun updateCharacters(deltaTime: Float) {
        for (character in charactersEntities) {
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
            val boardingComponent = ComponentsMapper.boarding.get(
                character
            )
            if (boardingComponent != null && boardingComponent.isBoarding()) {
                val stageTransform =
                    ComponentsMapper.modelInstance.get(gameSessionData.mapData.hangars[boardingComponent.color]).gameModelInstance.modelInstance.transform
                if (boardingComponent.isOffboarding()) {
                    if (stageTransform.getTranslation(auxVector1).y < MAX_Y) {
                        takeStepForElevatorWithCharacter(stageTransform, character, MAX_Y)
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
                            takeStepForElevatorWithCharacter(
                                stageTransform,
                                character,
                                StageComponent.BOTTOM_EDGE_Y
                            )
                        }
                        if (!isAlreadyDone) {
                            gamePlayManagers.dispatcher.dispatchMessage(
                                SystemEvents.CHARACTER_ONBOARDING_ANIMATION_DONE.ordinal,
                                character
                            )
                        }
                    }
                }
            } else {
                val hp = characterComponent.hp
                if (!characterComponent.dead && hp <= 0 && !ComponentsMapper.deathSequence.has(character)) {
                    gamePlayManagers.ecs.entityBuilder.addDeathSequenceComponentToEntity(
                        character,
                        minExplosions = 2,
                        maxExplosions = 4
                    )
                } else {
                    if (characterTransform.getTranslation(
                            auxVector1
                        ).y <= GameSessionDataMap.DROWNING_HEIGHT / 3
                    ) {
                        gamePlayManagers.dispatcher.dispatchMessage(
                            SystemEvents.CHARACTER_DIED.ordinal,
                            character
                        )
                    }
                    val smokeEmission = characterComponent.smokeEmission
                    if (hp <= definition.getHP() / 2F && smokeEmission == null) {
                        val position =
                            characterTransform.getTranslation(
                                auxVector1
                            )
                        val smoke =
                            gamePlayManagers.ecs.entityBuilder.begin().addParticleEffectComponent(
                                position = position,
                                pool = gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(
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
        val frontBoundingBox =
            gamePlayManagers.assetsManager.getCachedBoundingBox(ModelDefinition.APACHE_DEAD_FRONT)
        val backBoundingBox =
            gamePlayManagers.assetsManager.getCachedBoundingBox(ModelDefinition.APACHE_DEAD_BACK)
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
        addCharacterGiblet(
            gameModelInstanceBack,
            auxVector2.set(position).add(0.6F, 0F, 0F),
            backBoundingBox,
            backShape,
            planeCrashSoundId
        )
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
            pool = gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_UP_LOOP),
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
        addFire(position, part)
        val rigidBody = ComponentsMapper.physics.get(part).rigidBody
        pushRigidBodyRandomly(rigidBody, MathUtils.random(17F, 22F))
    }

    private fun addFire(position: Vector3, entity: Entity) {
        gamePlayManagers.ecs.entityBuilder.begin().addParticleEffectComponent(
            position = position,
            pool = gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.FIRE_LOOP),
            ttlInSeconds = MathUtils.random(20, 25),
            followSpecificEntity = entity
        ).finishAndAddToEngine()
    }

    private fun turnCharacterToCorpse(
        character: Entity,
        planeCrashSoundId: Long
    ) {
        val assetsManager = gamePlayManagers.assetsManager
        val characterComponent = ComponentsMapper.character.get(
            character
        )
        val characterDefinition = characterComponent.definition
        val deadGameModelInstance =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(
                characterDefinition.getCorpseModelDefinition()
            )
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(character)
        val position = modelInstanceComponent.gameModelInstance.modelInstance.transform.getTranslation(
            auxVector1
        )
        val color = characterComponent.color.name.lowercase()
        modelInstanceComponent.init(
            deadGameModelInstance,
            position,
            assetsManager.getCachedBoundingBox(ModelDefinition.APACHE_DEAD),
            0F,
            false,
            assetsManager.getTexture("${characterDefinition.getModelDefinition().name.lowercase()}_texture_dead_$color")
        )
        val rigidBody = ComponentsMapper.physics.get(character).rigidBody
        val motionState = rigidBody.motionState as MotionState
        val deadGameModelInstanceTransform = deadGameModelInstance.modelInstance.transform
        deadGameModelInstanceTransform.set(auxMatrix.set(rigidBody.worldTransform))
        motionState.transformObject = deadGameModelInstanceTransform
        motionState.setWorldTransform(deadGameModelInstanceTransform)
        rigidBody.linearFactor = Vector3(1F, 1F, 1F)
        rigidBody.angularFactor = Vector3(1F, 1F, 1F)
        if (characterDefinition == TurretCharacterDefinition.TANK) {
            rigidBody.friction = 1F
        }
        if (characterDefinition == SimpleCharacterDefinition.APACHE) {
            rigidBody.gravity = auxVector2.set(PhysicsComponent.worldGravity).scl(0.25F)
            pushRigidBodyRandomly(rigidBody, 12F)
            rigidBody.applyTorqueImpulse(
                Vector3(
                    MathUtils.random(),
                    MathUtils.random(),
                    MathUtils.random()
                ).scl(4F)
            )
            gamePlayManagers.ecs.entityBuilder.addCrashSoundEmitterComponentToEntity(
                character,
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.PLANE_CRASH),
                planeCrashSoundId
            )
        }
        character.remove(ChildDecalComponent::class.java)
        addFire(position, character)
        addSmokeUpToCharacter(character)
        if (characterDefinition.getCharacterType() == CharacterType.TURRET) {
            val turretCharacterDefinition = characterDefinition as TurretCharacterDefinition
            val turret = ComponentsMapper.turretBase.get(character).turret
            turret.remove(ChildModelInstanceComponent::class.java)
            val turretModelInstanceComponent =
                ComponentsMapper.modelInstance.get(turret)
            val randomDeadModel = turretCharacterDefinition.turretCorpseModelDefinitions.random()
            val oldModelInstance = turretModelInstanceComponent.gameModelInstance.modelInstance
            auxMatrix.set(oldModelInstance.transform)
            turretModelInstanceComponent.gameModelInstance = GameModelInstance(
                ModelInstance(gamePlayManagers.assetsManager.getAssetByDefinition(randomDeadModel)),
                randomDeadModel,
            )
            val modelInstance = turretModelInstanceComponent.gameModelInstance.modelInstance
            val turretDeadTextureName =
                "${characterDefinition.name.lowercase()}_turret_texture_destroyed_${color}"
            val turretDeadTexture = assetsManager.getTexture(turretDeadTextureName)
            modelInstance.materials[0].set(TextureAttribute.createDiffuse(turretDeadTexture))
            modelInstance.transform.set(auxMatrix)
            turretModelInstanceComponent.gameModelInstance.setBoundingBox(
                gamePlayManagers.assetsManager.getCachedBoundingBox(randomDeadModel)
            )
            gamePlayManagers.factories.specialEffectsFactory.generateExplosionForCharacter(
                character = turret,
            )
            val cannon = ComponentsMapper.turret.get(turret).cannon
            if (cannon != null) {
                val cannonModelInstanceComponent = ComponentsMapper.modelInstance.get(cannon)
                auxMatrix.set(cannonModelInstanceComponent.gameModelInstance.modelInstance.transform)
                val newGameModelInstance =
                    gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(ModelDefinition.TANK_CANNON_DESTROYED)
                cannonModelInstanceComponent.gameModelInstance = newGameModelInstance
                cannonModelInstanceComponent.gameModelInstance.setBoundingBox(
                    gamePlayManagers.assetsManager.getCachedBoundingBox(ModelDefinition.TANK_CANNON_DESTROYED)
                )
                newGameModelInstance.modelInstance.transform.set(auxMatrix)
                val cannonModelInstance = cannonModelInstanceComponent.gameModelInstance.modelInstance
                (cannonModelInstance.materials[0].get(TextureAttribute.Diffuse) as TextureAttribute).textureDescription.texture =
                    assetsManager.getTexture("${characterDefinition.name.lowercase()}_cannon_texture_destroyed_$color")
            }
        }
    }

    private fun addSmokeUpToCharacter(
        character: Entity,
    ) {
        val particleEffectsPools = gameSessionData.gamePlayData.pools.particleEffectsPools
        gamePlayManagers.ecs.entityBuilder.addParticleEffectComponentToEntity(
            entity = character,
            pool = particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_UP_LOOP),
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

    private fun takeStepForElevatorWithCharacter(
        elevatorTransform: Matrix4,
        character: Entity,
        targetY: Float
    ) {
        val elevatorBeforePosition = elevatorTransform.getTranslation(auxVector3)
        elevatorTransform.lerp(
            auxMatrix.idt().trn(elevatorBeforePosition.x, targetY, elevatorBeforePosition.z),
            0.06F
        )
        val newPosition = elevatorTransform.getTranslation(auxVector1)
        newPosition.y = if (abs(newPosition.y - targetY) < 0.03F) targetY else newPosition.y
        elevatorTransform.setTranslation(newPosition)
        ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.trn(
            0F,
            newPosition.y - elevatorBeforePosition.y,
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
            SystemEvents.CHARACTER_DEPLOYED.ordinal,
            character
        )
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
