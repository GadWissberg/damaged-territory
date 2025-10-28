package com.gadarts.returnfire.ecs.systems.character

import com.badlogic.ashley.core.*
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
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.ecs.components.CharacterComponent
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.ElevatorComponent
import com.gadarts.returnfire.ecs.components.ElevatorComponent.Companion.MAX_Y
import com.gadarts.returnfire.ecs.components.arm.ArmComponent
import com.gadarts.returnfire.ecs.components.model.GameModelInstance
import com.gadarts.returnfire.ecs.components.model.MutableGameModelInstanceInfo
import com.gadarts.returnfire.ecs.components.physics.MotionState
import com.gadarts.returnfire.ecs.components.physics.PhysicsComponent
import com.gadarts.returnfire.ecs.components.physics.RigidBody
import com.gadarts.returnfire.ecs.components.pit.HangarComponent
import com.gadarts.returnfire.ecs.systems.GameEntitySystem
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.character.handlers.CharacterAmbSoundHandler
import com.gadarts.returnfire.ecs.systems.character.handlers.TurretsHandler
import com.gadarts.returnfire.ecs.systems.character.react.*
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.ecs.systems.events.data.RemoveComponentEventData
import com.gadarts.returnfire.ecs.systems.render.RenderSystem
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.utils.CharacterPhysicsInitializer
import com.gadarts.returnfire.utils.ModelUtils
import com.gadarts.shared.SharedUtils
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition
import com.gadarts.shared.assets.definitions.SoundDefinition
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.ImmutableGameModelInstanceInfo
import com.gadarts.shared.data.definitions.characters.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.characters.TurretCharacterDefinition
import com.gadarts.shared.data.type.CharacterType
import kotlin.math.abs
import kotlin.math.sign

class CharacterSystemImpl(gamePlayManagers: GamePlayManagers) : CharacterSystem,
    GameEntitySystem(gamePlayManagers) {
    private val turretsHandler by lazy { TurretsHandler(gamePlayManagers, gameSessionData) }
    private val characterPhysicsInitializer = CharacterPhysicsInitializer(gamePlayManagers.assetsManager)
    private val characterAmbSoundHandler =
        CharacterAmbSoundHandler(gamePlayManagers.soundManager, gamePlayManagers.ecs.engine)
    private val charactersEntities: ImmutableArray<Entity> = gamePlayManagers.ecs.engine.getEntitiesFor(
        Family.all(CharacterComponent::class.java).get()
    )
    private val elevators: ImmutableArray<Entity> by lazy {
        gamePlayManagers.ecs.engine.getEntitiesFor(
            Family.all(ElevatorComponent::class.java).get()
        )
    }
    private val hangars: ImmutableArray<Entity> by lazy {
        gamePlayManagers.ecs.engine.getEntitiesFor(
            Family.all(HangarComponent::class.java).get()
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> by lazy {
        mapOf(
            SystemEvents.PHYSICS_DROWNED to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val character = msg.extraInfo as Entity
                    val characterComponent = ComponentsMapper.character.get(character) ?: return

                    if (!characterComponent.dead) {
                        gamePlayManagers.dispatcher.dispatchMessage(
                            SystemEvents.CHARACTER_DIED.ordinal,
                            character
                        )
                    }

                    gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.REMOVE_ENTITY.ordinal, character)
                }
            },
            SystemEvents.CHARACTER_WEAPON_ENGAGED_PRIMARY to CharacterSystemOnCharacterWeaponShotPrimary(this),
            SystemEvents.CHARACTER_WEAPON_ENGAGED_SECONDARY to CharacterSystemOnCharacterWeaponShotSecondary(this),
            SystemEvents.PHYSICS_COLLISION to CharacterSystemOnPhysicsCollision(
                gamePlayManagers.ctfManager
            ),
            SystemEvents.CHARACTER_REQUEST_BOARDING to CharacterSystemOnCharacterRequestBoarding(),
            SystemEvents.AMB_SOUND_COMPONENT_ADDED to CharacterSystemOnAmbSoundComponentAdded(this),
            SystemEvents.OPPONENT_ENTERED_GAME_PLAY_SCREEN to CharacterSystemOnOpponentEnteredGamePlayScreen(
                gamePlayManagers.ecs.engine,
            ),
            SystemEvents.MAP_SYSTEM_READY to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val elevatorEntities =
                        engine.getEntitiesFor(Family.all(ElevatorComponent::class.java).get())
                    gameSessionData.mapData.elevators =
                        elevatorEntities.associateBy(
                            { ComponentsMapper.hangar.get(ComponentsMapper.elevator.get(it).hangar).color },
                            { it })
                }
            },
            SystemEvents.CHARACTER_DEATH_SEQUENCE_FINISHED to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    if (!ComponentsMapper.character.has(msg.extraInfo as Entity)) return

                    destroyCharacter(character = msg.extraInfo as Entity)
                }
            },
            SystemEvents.CHARACTER_ONBOARDING_BEGIN to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val character = msg.extraInfo as Entity
                    val turretBaseComponent = ComponentsMapper.turretBase.get(character) ?: return

                    val turretAutomationComponent =
                        ComponentsMapper.turretAutomation.get(turretBaseComponent.turret)
                    if (turretAutomationComponent != null) {
                        turretAutomationComponent.enabled = false
                    }
                    val characterComponent = ComponentsMapper.character.get(character)
                    if (characterComponent.definition == TurretCharacterDefinition.JEEP) {
                        val rivalFlag =
                            gameSessionData.gamePlayData.flags[if (characterComponent.color == CharacterColor.GREEN) CharacterColor.BROWN else CharacterColor.GREEN]
                        if (ComponentsMapper.flag.get(rivalFlag).follow == character && rivalFlag != null) {
                            gamePlayManagers.ctfManager.returnFlag(rivalFlag)
                        }
                    }
                }
            },
            SystemEvents.ELEVATOR_EMPTY_ONBOARD_REQUESTED to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val color = msg.extraInfo as CharacterColor
                    val elevator = gameSessionData.mapData.elevators[color] ?: return

                    val elevatorComponent = ComponentsMapper.elevator.get(elevator)
                    ComponentsMapper.hangar.get(elevatorComponent.hangar).close()
                    elevatorComponent.emptyOnboard = true
                }
            })
    }

    private fun destroyCharacter(
        character: Entity,
    ) {
        if (ComponentsMapper.boarding.has(character) && ComponentsMapper.boarding.get(character).isBoarding()) return

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
        var planeCrashSoundId = -1L
        val assetsManager = gamePlayManagers.assetsManager
        if (characterComponent.definition.isFlyer()) {
            planeCrashSoundId = gamePlayManagers.soundManager.play(
                assetsManager.getAssetByDefinition(SoundDefinition.PLANE_CRASH),
                ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            )
        }
        gamePlayManagers.dispatcher.dispatchMessage(
            SystemEvents.CHARACTER_DIED.ordinal,
            character
        )
        if (!assetsManager.gameSettings.forceGibs && (!characterComponent.definition.isGibable() || MathUtils.random() >= 0.5F)) {
            turnCharacterToCorpse(character, planeCrashSoundId)
        } else {
            gibCharacter(character, planeCrashSoundId)
        }
        removeCharacterIfOnHangar(character)
        val opponentData = gameSessionData.gamePlayData.opponentsData[characterComponent.color]
        if (opponentData != null) {
            opponentData.vehicleAmounts[characterComponent.definition] =
                opponentData.vehicleAmounts[characterComponent.definition]!! - 1
        }
    }

    private fun removeCharacterIfOnHangar(character: Entity) {
        val characterGameModelInstance = ComponentsMapper.modelInstance.get(character).gameModelInstance
        characterGameModelInstance.getBoundingBox(auxBoundingBox2)
            .mul(characterGameModelInstance.modelInstance.transform)
        for (hangar in hangars) {
            val modelInstance = ComponentsMapper.modelInstance.get(hangar)
            val hangarBoundingBox = modelInstance.gameModelInstance.getBoundingBox(auxBoundingBox1)
                .mul(modelInstance.gameModelInstance.modelInstance.transform)
            if (hangarBoundingBox.intersects(auxBoundingBox2)) {
                gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.REMOVE_ENTITY.ordinal, character)
                break
            }
        }
    }

    override fun initialize(gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        super.initialize(gameSessionData, gamePlayManagers)
        engine.addEntityListener(object : EntityListener {
            override fun entityAdded(entity: Entity) {
                playAmbSound(entity, gamePlayManagers)
            }

            override fun entityRemoved(entity: Entity) {
                val turretBaseComponent = ComponentsMapper.turretBase.get(entity)
                val dispatcher = gamePlayManagers.dispatcher
                if (turretBaseComponent != null) {
                    val turret = turretBaseComponent.turret
                    if (turret != null) {
                        dispatcher.dispatchMessage(SystemEvents.REMOVE_ENTITY.ordinal, turret)
                    }
                } else {
                    val turretComponent = ComponentsMapper.turret.get(entity)
                    if (turretComponent != null) {
                        dispatcher.dispatchMessage(SystemEvents.REMOVE_ENTITY.ordinal, turretComponent.cannon)
                    }
                }
            }

        })
    }

    override fun resume(delta: Long) {

    }

    override fun update(deltaTime: Float) {
        if (isGamePaused()) return

        updateCharacters(deltaTime)
        turretsHandler.update(deltaTime)
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
        if (!gamePlayManagers.assetsManager.gameSettings.disableAmbSounds && ComponentsMapper.ambSound.has(entity)) {
            val ambSoundComponent = ComponentsMapper.ambSound.get(entity)
            if (ambSoundComponent.soundId == -1L) {
                val id = gamePlayManagers.soundManager.loopSound(ambSoundComponent.sound)
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
            if (boardingComponent != null && boardingComponent.isBoarding() && TimeUtils.timeSinceMillis(
                    boardingComponent.creationTime
                ) > 1000F
            ) {
                val hangar = gameSessionData.mapData.elevators[boardingComponent.color]
                val elevatorTransform =
                    ComponentsMapper.modelInstance.get(hangar).gameModelInstance.modelInstance.transform
                if (boardingComponent.isDeploying()) {
                    if (elevatorTransform.getTranslation(auxVector1).y < MAX_Y) {
                        takeStepForElevatorWithCharacter(elevatorTransform, character, MAX_Y, deltaTime)
                    } else {
                        val animationDone = updateBoardingAnimation(deltaTime, character)
                        if (animationDone && boardingComponent.isDeploying()) {
                            deployingDone(character)
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
                            character
                        )
                        character.remove(PhysicsComponent::class.java)
                    }
                    if (animationDone) {
                        val elevatorPosition = elevatorTransform.getTranslation(auxVector1)
                        if (elevatorPosition.y <= ElevatorComponent.BOTTOM_EDGE_Y) {
                            gamePlayManagers.dispatcher.dispatchMessage(
                                SystemEvents.CHARACTER_ONBOARDING_FINISHED.ordinal,
                                character
                            )
                            elevatorTransform.setTranslation(
                                elevatorPosition.x,
                                ElevatorComponent.BOTTOM_EDGE_Y,
                                elevatorPosition.z
                            )
                            gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.REMOVE_ENTITY.ordinal, character)
                        } else {
                            takeStepForElevatorWithCharacter(
                                elevatorTransform,
                                character,
                                ElevatorComponent.BOTTOM_EDGE_Y,
                                deltaTime
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
                if (!ComponentsMapper.deathSequence.has(character)
                    && ((!characterComponent.dead && hp <= 0) || characterComponent.fuel <= 0)
                ) {
                    gamePlayManagers.ecs.entityBuilder.addDeathSequenceComponentToEntity(
                        character,
                        minExplosions = 2,
                        maxExplosions = 4
                    )
                } else {
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
        for (elevator in elevators) {
            val elevatorComponent = ComponentsMapper.elevator.get(elevator)
            if (elevatorComponent.emptyOnboard) {
                takeStepForElevator(
                    ComponentsMapper.modelInstance.get(elevator).gameModelInstance.modelInstance.transform,
                    ElevatorComponent.BOTTOM_EDGE_Y,
                    deltaTime
                )
                if (ModelUtils.getPositionOfModel(elevator).y <= ElevatorComponent.BOTTOM_EDGE_Y) {
                    elevatorComponent.emptyOnboard = false
                    gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.ELEVATOR_EMPTY_ONBOARD.ordinal, elevator)
                }
            }
        }
    }

    private fun gibCharacter(character: Entity, planeCrashSoundId: Long) {
        val position =
            ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val customTexture = "apache_texture_dead_${ComponentsMapper.character.get(character).color.name.lowercase()}"
        val gameModelInstanceBack =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(
                ModelDefinition.APACHE_DEAD_BACK,
                customTexture
            )
        val gameModelInstanceFront =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(
                ModelDefinition.APACHE_DEAD_FRONT,
                customTexture
            )
        val assetsManager = gamePlayManagers.assetsManager
        val frontBoundingBox =
            assetsManager.getCachedBoundingBox(ModelDefinition.APACHE_DEAD_FRONT)
        val backBoundingBox =
            assetsManager.getCachedBoundingBox(ModelDefinition.APACHE_DEAD_BACK)
        val backShape = btCompoundShape()
        val btBackShapeMain = btBoxShape(Vector3(0.2F, 0.075F, 0.1F))
        val btBackShapeHorTail = btBoxShape(Vector3(0.05F, 0.05F, 0.135F))
        val btBackShapeVertTail = btBoxShape(Vector3(0.06F, 0.1F, 0.12F))
        backShape.addChildShape(Matrix4(), btBackShapeMain)
        backShape.addChildShape(Matrix4().translate(-0.43F, 0F, 0.12F), btBackShapeHorTail)
        backShape.addChildShape(Matrix4().translate(-0.47F, 0F, 0.08F), btBackShapeHorTail)
        backShape.addChildShape(Matrix4().translate(-0.43F, 0.1F, 0.12F), btBackShapeVertTail)
        addCharacterGiblet(
            gameModelInstanceBack,
            auxVector2.set(position).add(0.6F, 0F, 0F),
            backBoundingBox,
            SharedUtils.buildShapeFromModelCollisionShapeInfo(
                assetsManager.getCachedModelCollisionShapeInfo(
                    auxGameModelInstanceInfo.set(
                        ModelDefinition.APACHE_DEAD_BACK,
                        null
                    )
                )!!
            ),
            planeCrashSoundId
        )
        addCharacterGiblet(
            gameModelInstanceFront,
            position,
            frontBoundingBox,
            SharedUtils.buildShapeFromModelCollisionShapeInfo(
                assetsManager.getCachedModelCollisionShapeInfo(
                    auxGameModelInstanceInfo.set(
                        ModelDefinition.APACHE_DEAD_FRONT,
                        null
                    )
                )!!
            ),
            planeCrashSoundId
        )
        gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.REMOVE_ENTITY.ordinal, character)
    }

    private fun addCharacterGiblet(
        gameModelInstanceBack: GameModelInstance,
        position: Vector3,
        boundingBox: BoundingBox,
        btBoxShape: btCollisionShape,
        planeCrashSoundId: Long
    ) {
        val assetsManager = gamePlayManagers.assetsManager
        val part = gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                model = gameModelInstanceBack,
                position = position,
                boundingBox = boundingBox,
            )
            .addDrowningEffectComponent()
            .addParticleEffectComponent(
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
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(character)
        val isTurretCannon = characterDefinition == TurretCharacterDefinition.TURRET_CANNON
        val deadGameModelInstance = if (!isTurretCannon)
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(
                characterDefinition.getCorpseModelDefinitions().random()
            ) else modelInstanceComponent.gameModelInstance
        val position = modelInstanceComponent.gameModelInstance.modelInstance.transform.getTranslation(
            auxVector1
        )
        val color = characterComponent.color.name.lowercase()
        if (!isTurretCannon) {
            modelInstanceComponent.gameModelInstance.modelInstance.transform.setTranslation(position)
            modelInstanceComponent.init(
                deadGameModelInstance,
                position,
                assetsManager.getCachedBoundingBox(ModelDefinition.APACHE_DEAD),
                0F,
                false,
                assetsManager.getTexture("${characterDefinition.getModelDefinition().name.lowercase()}_texture_dead_$color"),
                null
            )
        }
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
        removeComponent(character, ComponentsMapper.childDecal)
        addFire(position, character)
        addSmokeUpToCharacter(character)
        if (characterDefinition.getCharacterType() == CharacterType.TURRET) {
            val turretCharacterDefinition = characterDefinition as TurretCharacterDefinition
            val turretBaseComponent = ComponentsMapper.turretBase.get(character)
            val turret = turretBaseComponent.turret
            if (turret != null) {
                val dispatcher = gamePlayManagers.dispatcher
                removeComponent(turret, ComponentsMapper.childModelInstance)
                val turretModelInstanceComponent =
                    ComponentsMapper.modelInstance.get(turret)
                val turretCorpseModelDefinitions = turretCharacterDefinition.turretCorpseModelDefinitions
                val cannon = ComponentsMapper.turret.get(turret).cannon
                if (turretCorpseModelDefinitions.isNotEmpty()) {
                    val randomDeadModel = turretCorpseModelDefinitions.random()
                    val oldModelInstance = turretModelInstanceComponent.gameModelInstance.modelInstance
                    auxMatrix.set(oldModelInstance.transform)
                    turretModelInstanceComponent.gameModelInstance = GameModelInstance(
                        ModelInstance(gamePlayManagers.assetsManager.getAssetByDefinition(randomDeadModel)),
                        ImmutableGameModelInstanceInfo(randomDeadModel),
                    )
                    val modelInstance = turretModelInstanceComponent.gameModelInstance.modelInstance
                    val separateTextureForDeadTurret = turretCharacterDefinition.separateTextureForDeadTurret
                    if (separateTextureForDeadTurret) {
                        val turretDeadTextureName =
                            "${characterDefinition.name.lowercase()}_turret_texture_destroyed_${color}"
                        val turretDeadTexture = assetsManager.getTexture(turretDeadTextureName)
                        modelInstance.materials[0].set(TextureAttribute.createDiffuse(turretDeadTexture))
                    }
                    modelInstance.transform.set(auxMatrix)
                    turretModelInstanceComponent.gameModelInstance.setBoundingBox(
                        gamePlayManagers.assetsManager.getCachedBoundingBox(randomDeadModel)
                    )
                    gamePlayManagers.factories.specialEffectsFactory.generateExplosionForCharacter(
                        character = turret,
                    )
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
                        if (separateTextureForDeadTurret) {
                            val cannonModelInstance = cannonModelInstanceComponent.gameModelInstance.modelInstance
                            (cannonModelInstance.materials[0].get(TextureAttribute.Diffuse) as TextureAttribute).textureDescription.texture =
                                assetsManager.getTexture("${characterDefinition.name.lowercase()}_cannon_texture_destroyed_$color")
                        }
                    }
                } else {
                    dispatcher.dispatchMessage(SystemEvents.REMOVE_ENTITY.ordinal, turret)
                    dispatcher.dispatchMessage(SystemEvents.REMOVE_ENTITY.ordinal, cannon)
                    turretBaseComponent.turret = null
                }
            }
        }
    }

    private fun removeComponent(
        entity: Entity,
        componentMapper: ComponentMapper<out Component>,
    ) {
        val component = componentMapper.get(entity) ?: return

        RemoveComponentEventData.set(entity, component)
        gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.REMOVE_COMPONENT.ordinal)
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
        targetY: Float,
        deltaTime: Float
    ) {
        val deltaMovement = takeStepForElevator(elevatorTransform, targetY, deltaTime)

        // Move character along with elevator
        ComponentsMapper.modelInstance.get(character)
            .gameModelInstance.modelInstance.transform.trn(0f, deltaMovement, 0f)
    }

    private fun takeStepForElevator(
        elevatorTransform: Matrix4,
        targetY: Float,
        deltaTime: Float
    ): Float {
        val currentPosition = elevatorTransform.getTranslation(auxVector3)
        val y = currentPosition.y
        if ((y <= targetY && targetY == ElevatorComponent.BOTTOM_EDGE_Y) || (y >= MAX_Y && targetY == MAX_Y)) return 0f

        val distance = targetY - y
        val direction = distance.sign
        val absDistance = abs(distance)

        val isOnboarding = distance < 0f

        val maxSpeed = if (isOnboarding) 0.6f else 1.5f
        val minSpeed = if (isOnboarding) 0.2f else 0.3f  // Define your desired minimal constant speed
        val slowDownDistance = if (isOnboarding) 1.0f else 0.5f  // Distance at which slowing starts

        // Calculate speed with smooth deceleration
        val targetSpeed = if (absDistance < slowDownDistance) {
            val t = absDistance / slowDownDistance
            val easedT = t * t * (3f - 2f * t)
            minSpeed + (maxSpeed - minSpeed) * easedT
        } else {
            maxSpeed
        }

        // Calculate actual movement this frame, ensuring we don't overshoot
        val movementThisFrame = (targetSpeed * deltaTime).coerceAtMost(absDistance)

        // Update elevator position
        val deltaMovement = movementThisFrame * direction
        currentPosition.y += deltaMovement
        elevatorTransform.setTranslation(currentPosition)
        return deltaMovement
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
            gamePlayManagers.soundManager,
            gamePlayManagers.assetsManager
        )
    }

    private fun deployingDone(character: Entity) {
        ComponentsMapper.boarding.get(character).boardingDone()
        characterPhysicsInitializer.initialize(
            gamePlayManagers.ecs.entityBuilder,
            character,
        )
        val turretBaseComponent = ComponentsMapper.turretBase.get(character)
        if (turretBaseComponent != null) {
            val automationComponent = ComponentsMapper.turretAutomation.get(turretBaseComponent.turret)
            if (automationComponent != null && !automationComponent.enabled) {
                automationComponent.enabled = true
            }
        }
        gamePlayManagers.dispatcher.dispatchMessage(
            SystemEvents.CHARACTER_DEPLOYMENT_DONE.ordinal,
            character
        )
    }


    companion object {
        const val ROT_STEP = 1600F
        private val auxMatrix = Matrix4()
        private val auxQuat = Quaternion()
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxGameModelInstanceInfo = MutableGameModelInstanceInfo()
        private val auxBoundingBox1 = BoundingBox()
        private val auxBoundingBox2 = BoundingBox()
    }

}
