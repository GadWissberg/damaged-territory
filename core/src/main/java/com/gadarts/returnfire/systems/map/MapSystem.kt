package com.gadarts.returnfire.systems.map

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.Collision
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition.*
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.*
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.cd.DecalAnimation
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.physics.RigidBody
import com.gadarts.returnfire.factories.SpecialEffectsFactory
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.definitions.AmbDefinition
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData
import com.gadarts.returnfire.systems.physics.BulletEngineHandler.Companion.COLLISION_GROUP_GENERAL
import com.gadarts.returnfire.utils.GeneralUtils
import com.gadarts.returnfire.utils.MapInflater
import kotlin.math.max
import kotlin.math.min

class MapSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {

    private val fallingBuildingsToRemove = ArrayList<Entity>()
    private val fallingBuildings = ArrayList<Entity>()
    private val waterSplashHandler = WaterSplashHandler(gamePlayManagers.ecs.engine)
    private val fadingAwayHandler: FadingAwayHandler = FadingAwayHandler(gamePlayManagers.ecs.engine)
    private val mapSystemRelatedEntities = MapSystemRelatedEntities(gamePlayManagers.ecs.engine)
    private val groundTextureAnimationHandler = GroundTextureAnimationHandler(gamePlayManagers.ecs.engine)
    private val landingMark: ChildDecal by lazy { createLandingMark() }
    private val ambSoundsHandler = AmbSoundsHandler()

    private val bases: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(
                BaseComponent::class.java,
            ).get()
        )
    }


    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> =
        mapOf(
            SystemEvents.PHYSICS_COLLISION to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val entity0 = PhysicsCollisionEventData.colObj0.userData as Entity
                    val entity1 = PhysicsCollisionEventData.colObj1.userData as Entity
                    handleCollisionRoadWithBullets(
                        entity0,
                        entity1
                    ) || handleCollisionRoadWithBullets(
                        entity1,
                        entity0
                    ) || handleCollisionGroundWithHeavyStuffOnHighSpeed(
                        entity0,
                        entity1
                    ) || handleCollisionGroundWithHeavyStuffOnHighSpeed(
                        entity1,
                        entity0
                    ) || handleCollisionDestroyableAmbWithFastAndHeavyStuff(
                        entity0,
                        entity1
                    ) || handleCollisionDestroyableAmbWithFastAndHeavyStuff(
                        entity1,
                        entity0
                    )
                }

            },
            SystemEvents.PHYSICS_DROWNING to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val entity = msg.extraInfo as Entity
                    val modelInstanceComponent = ComponentsMapper.modelInstance.get(entity) ?: return
                    if (entity.isRemoving || entity.isScheduledForRemoval) return

                    val position =
                        modelInstanceComponent.gameModelInstance.modelInstance.transform.getTranslation(
                            auxVector1
                        )
                    position.set(
                        position.x + MathUtils.randomSign() * MathUtils.random(0.2F),
                        SpecialEffectsFactory.WATER_SPLASH_Y,
                        position.z + MathUtils.randomSign() * MathUtils.random(0.2F)
                    )
                    gamePlayManagers.factories.specialEffectsFactory.generateWaterSplash(
                        position, ComponentsMapper.character.has(entity)
                    )
                    val physicsComponent = ComponentsMapper.physics.get(
                        entity
                    )
                    val rigidBody = physicsComponent.rigidBody
                    gameSessionData.physicsData.collisionWorld.removeRigidBody(
                        rigidBody
                    )
                    engine.removeEntity(entity)
                }
            },
            SystemEvents.CHARACTER_ONBOARDING_ANIMATION_DONE to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val base = findBase(msg.extraInfo as Entity)
                    closeDoors(base, gamePlayManagers)
                    landingMark.visible = false
                }
            },
            SystemEvents.CHARACTER_BOARDING to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val boardingComponent = ComponentsMapper.boarding.get(gameSessionData.gamePlayData.player)
                    if (boardingComponent.boardingAnimation == null && boardingComponent.isOnboarding()) {
                        val base = findBase(msg.extraInfo as Entity)
                        closeDoors(base, gamePlayManagers)
                    }
                }
            },
            SystemEvents.EXPLOSION_PUSH_BACK to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val position = msg.extraInfo as Vector3
                    applyExplosionPushBackOnEnvironment(
                        position,
                        mapSystemRelatedEntities.flyingPartEntities,
                        FlyingPartExplosionPushBackEffect
                    )
                    applyExplosionPushBackOnEnvironment(
                        position,
                        mapSystemRelatedEntities.treeEntities,
                        TreeExplosionPushBackEffect
                    )
                }
            },
            SystemEvents.DEATH_SEQUENCE_FINISHED to object : HandlerOnEvent {
                override fun react(
                    msg: Telegram,
                    gameSessionData: GameSessionData,
                    gamePlayManagers: GamePlayManagers
                ) {
                    val entity = msg.extraInfo as Entity
                    if (!ComponentsMapper.amb.has(entity)) return

                    collapseBuilding(entity, gameSessionData, gamePlayManagers)
                }
            })

    private fun collapseBuilding(
        entity: Entity,
        gameSessionData: GameSessionData,
        gamePlayManagers: GamePlayManagers
    ) {
        val rigidBody = ComponentsMapper.physics.get(entity).rigidBody
        val collisionWorld = gameSessionData.physicsData.collisionWorld
        collisionWorld.removeRigidBody(rigidBody)
        rigidBody.collisionFlags = CollisionFlags.CF_NO_CONTACT_RESPONSE
        collisionWorld.addRigidBody(
            rigidBody,
            COLLISION_GROUP_GENERAL,
            0
        )
        rigidBody.gravity = auxVector1.set(0F, -0.5F, 0F)
        rigidBody.activationState = Collision.DISABLE_DEACTIVATION
        fallingBuildings.add(entity)
        gamePlayManagers.soundPlayer.play(
            gamePlayManagers.assetsManager.getAssetByDefinition(
                SoundDefinition.FALLING_BUILDING
            )
        )
    }

    object TreeExplosionPushBackEffect : ExplosionPushBackEffect {
        override fun go(entity: Entity, affectedEntityPosition: Vector3, explosionPosition: Vector3) {
            ComponentsMapper.ambAnimation.get(entity)?.applyAffectedByExplosionAnimation()
        }

    }

    object FlyingPartExplosionPushBackEffect : ExplosionPushBackEffect {
        override fun go(entity: Entity, affectedEntityPosition: Vector3, explosionPosition: Vector3) {
            ComponentsMapper.physics.get(entity).rigidBody.applyCentralImpulse(
                auxVector3.set(affectedEntityPosition).sub(explosionPosition).nor()
                    .scl(MathUtils.random(2F, 4F)),
            )
        }

    }

    interface ExplosionPushBackEffect {
        fun go(entity: Entity, affectedEntityPosition: Vector3, explosionPosition: Vector3)

    }

    private fun applyExplosionPushBackOnEnvironment(
        position: Vector3,
        entities: ImmutableArray<Entity>,
        effect: ExplosionPushBackEffect
    ) {
        for (entity in entities) {
            val touchedEntityPosition =
                ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            if (touchedEntityPosition.dst2(position) < 3F) {
                effect.go(entity, touchedEntityPosition, position)
            }
        }
    }


    private fun handleCollisionDestroyableAmbWithFastAndHeavyStuff(entity0: Entity, entity1: Entity): Boolean {
        val ambComponent = ComponentsMapper.amb.get(entity0) ?: return false

        if (ambComponent.def.hp > 0 && ambComponent.hp > 0) {
            val otherRigidBody = ComponentsMapper.physics.get(entity1).rigidBody
            val bulletComponent = ComponentsMapper.bullet.get(entity1)
            if (!ambComponent.def.destroyedByExplosiveOnly) {
                destroyLightweightAmb(otherRigidBody, bulletComponent, entity0, ambComponent)
            } else if (bulletComponent != null && bulletComponent.explosive) {
                handleCollisionHealthyAmbWithFastAndHeavyStuff(entity0, entity1)
            }
        }
        return true
    }

    private fun destroyLightweightAmb(
        otherRigidBody: RigidBody,
        bulletComponent: BulletComponent?,
        affectedEntity: Entity,
        ambComponent: AmbComponent
    ) {
        val otherSpeed = otherRigidBody.linearVelocity.len2()
        val isExplosive = bulletComponent != null && bulletComponent.explosive
        val position =
            ComponentsMapper.modelInstance.get(affectedEntity).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        if (otherSpeed > 10F || (otherSpeed > 0.1F && otherRigidBody.mass > 7)) {
            ambComponent.hp = 0
            if (bulletComponent != null) {
                if (isExplosive) {
                    if (MathUtils.random() >= 0.5F) {
                        createIndependentParticleEffect(position, FIRE_LOOP_SMALL)
                    }
                } else {
                    createIndependentParticleEffect(position, SMOKE)
                }
                if (ambComponent.def == AmbDefinition.PALM_TREE) {
                    destroyTree(affectedEntity, isExplosive)
                }
            }
            if ((ambComponent.def.stayOnDeath)) {
                val rigidBody = ComponentsMapper.physics.get(affectedEntity).rigidBody
                rigidBody.activationState = Collision.DISABLE_DEACTIVATION
                rigidBody.collisionFlags = CollisionFlags.CF_CHARACTER_OBJECT
                if (ambComponent.def == AmbDefinition.STREET_LIGHT) {
                    gamePlayManagers.ecs.entityBuilder.begin().addParticleEffectComponent(
                        position,
                        gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(
                            SPARK_MED
                        )
                    ).finishAndAddToEngine()
                }
            } else {
                destroyAmbObject(affectedEntity)
                createFlyingPartsForAmb(affectedEntity, auxVector3.set(0F, 0.5F, 0F))
            }
        }
    }

    private fun handleCollisionHealthyAmbWithFastAndHeavyStuff(
        amb: Entity,
        otherCollider: Entity
    ) {
        val ambComponent = ComponentsMapper.amb.get(amb)
        val gameModelInstance = ComponentsMapper.modelInstance.get(amb).gameModelInstance
        val position =
            gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val beforeHp = ambComponent.hp
        ambComponent.hp -= MathUtils.random(1, 2)
        val newHp = ambComponent.hp
        createFlyingPartsForAmb(otherCollider, Vector3.Zero)
        if (newHp <= 0) {
            initiateAmbDestruction(amb, position)
        } else {
            createSmokeForBuilding(
                ambComponent,
                ambComponent.def == AmbDefinition.BUILDING_0,
                beforeHp,
                newHp,
                gameModelInstance
            )
        }
    }

    private fun createFlyingPartsForAmb(
        entity: Entity,
        relativeOffset: Vector3
    ) {
        val specialEffectsFactory = gamePlayManagers.factories.specialEffectsFactory
        val ambComponent = ComponentsMapper.amb.get(entity)
        if (ambComponent?.def?.flyingPart != null) {
            specialEffectsFactory.generateFlyingParts(
                entity,
                ambComponent.def.flyingPart,
                min = ambComponent.def.minFlyingParts,
                max = ambComponent.def.maxFlyingParts,
                minForce = ambComponent.def.flyingPartMinImpulse,
                maxForce = ambComponent.def.flyingPartMaxImpulse,
                relativeOffset = relativeOffset
            )
        } else {
            specialEffectsFactory.generateFlyingParts(entity)
        }
    }

    private fun initiateAmbDestruction(
        amb: Entity,
        position: Vector3
    ) {
        val ambComponent = ComponentsMapper.amb.get(amb)
        val def = ambComponent.def
        val factories = gamePlayManagers.factories
        if (def.flyingPart != null) {
            factories.specialEffectsFactory.generateFlyingParts(
                character = amb,
                modelDefinition = def.flyingPart,
                min = def.minFlyingParts,
                max = def.minFlyingParts,
                mass = 0.5F
            )
        }
        createIndependentParticleEffect(position, SMOKE)
        gamePlayManagers.soundPlayer.play(
            gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION),
            position
        )
        if (def.destructionSound != null) {
            gamePlayManagers.soundPlayer.play(
                gamePlayManagers.assetsManager.getAssetByDefinition(def.destructionSound),
                position
            )
        }
        if (def.hasDeathSequence) {
            gamePlayManagers.ecs.entityBuilder.addDeathSequenceComponentToEntity(amb, true, 6, 9)
            createIndependentParticleEffect(position.add(0F, 0F, 0F), SMOKE_BIG_RECTANGLE, ttlInSeconds = 0)
        } else {
            when (def) {
                AmbDefinition.ANTENNA -> {
                    initiateAntennaDestruction(amb)
                }

                AmbDefinition.ROCK_BIG -> {
                    initiateRockBigDestruction(amb)
                }

                AmbDefinition.WATCH_TOWER -> {
                    initiateWatchTowerDestruction(amb)
                }

                else -> {}
            }
            destroyAmbObject(amb)
        }
    }

    private fun initiateWatchTowerDestruction(amb: Entity) {
        gamePlayManagers.factories.specialEffectsFactory.generateFlyingParts(
            character = amb,
            modelDefinition = ModelDefinition.ROCK_PART_BIG,
            min = 2,
            max = 3,
            mass = 3F,
            minForce = 8F,
            maxForce = 14F
        )
        createAmbCorpsePart(
            ModelDefinition.WATCH_TOWER_DESTROYED,
            amb,
            auxVector1.set(
                0F,
                gamePlayManagers.assetsManager.getCachedBoundingBox(ModelDefinition.WATCH_TOWER).height / 2F,
                0F
            ),
            6F,
            12F
        )
    }

    private fun initiateRockBigDestruction(amb: Entity) {
        gamePlayManagers.factories.specialEffectsFactory.generateFlyingParts(
            character = amb,
            modelDefinition = ModelDefinition.ROCK_PART_BIG,
            min = 2,
            max = 3,
            mass = 3F,
            minForce = 10F,
            maxForce = 16F
        )
    }

    private fun initiateAntennaDestruction(amb: Entity) {
        createAntennaBase(amb)
        createAmbCorpsePart(ModelDefinition.ANTENNA_DESTROYED_BODY, amb, auxVector1.setZero(), 1.5F, 7F)
    }

    private fun createAmbCorpsePart(
        modelDefinition: ModelDefinition,
        amb: Entity,
        relativePosition: Vector3,
        impulse: Float,
        mass: Float
    ) {
        auxMatrix.set(ComponentsMapper.modelInstance.get(amb).gameModelInstance.modelInstance.transform)
        val bodyGameModelInstance =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(modelDefinition)
        val cachedBoundingBox =
            gamePlayManagers.assetsManager.getCachedBoundingBox(modelDefinition)
        val def = ComponentsMapper.amb.get(amb).def
        val ambCorpsePart = gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                bodyGameModelInstance,
                Vector3.Zero,
                cachedBoundingBox
            ).addAmbCorpsePart(def.corpsePartDestroyOnGroundImpact, def.corpsePartCollisionSound)
            .addPhysicsComponent(
                modelDefinition.physicalShapeCreator!!.create(),
                CollisionFlags.CF_CHARACTER_OBJECT,
                bodyGameModelInstance.modelInstance.transform.set(auxMatrix).trn(
                    0F,
                    cachedBoundingBox.height / 2F,
                    0F
                ).trn(relativePosition),
                0.5F,
                mass
            ).finishAndAddToEngine()
        ComponentsMapper.physics.get(ambCorpsePart).rigidBody.applyImpulse(
            auxVector1.setToRandomDirection().scl(impulse), auxVector2.setToRandomDirection()
        )

    }

    private fun createAntennaBase(amb: Entity) {
        auxMatrix.set(ComponentsMapper.modelInstance.get(amb).gameModelInstance.modelInstance.transform)
        val baseGameModelInstance =
            gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(ModelDefinition.ANTENNA_DESTROYED_BASE)
        gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                baseGameModelInstance,
                Vector3.Zero,
                gamePlayManagers.assetsManager.getCachedBoundingBox(ModelDefinition.ANTENNA_DESTROYED_BASE)
            ).addPhysicsComponent(
                ModelDefinition.ANTENNA_DESTROYED_BASE.physicalShapeCreator!!.create(),
                CollisionFlags.CF_STATIC_OBJECT,
                baseGameModelInstance.modelInstance.transform.set(auxMatrix),
                1F,
                0F
            ).finishAndAddToEngine()
    }

    private fun createSmokeForBuilding(
        ambComponent: AmbComponent,
        isBuilding: Boolean,
        beforeHp: Int,
        newHp: Int,
        gameModelInstance: GameModelInstance,
    ) {
        val half = ambComponent.def.hp / 2F
        if (isBuilding && beforeHp >= half && newHp < half) {
            gameModelInstance.getBoundingBox(auxBoundingBox)
            val particleEffectDefinition = SMOKE_LOOP_BIG
            createIndependentParticleEffect(
                GeneralUtils.getRandomPositionOnBoundingBox(auxBoundingBox, 0F),
                particleEffectDefinition,
                0
            )
            createIndependentParticleEffect(
                GeneralUtils.getRandomPositionOnBoundingBox(auxBoundingBox, 0F),
                particleEffectDefinition, 0
            )
            createIndependentParticleEffect(
                GeneralUtils.getRandomPositionOnBoundingBox(auxBoundingBox, 0F),
                particleEffectDefinition,
                0
            )
            createIndependentParticleEffect(
                GeneralUtils.getRandomPositionOnBoundingBox(auxBoundingBox, 0F),
                particleEffectDefinition, 0
            )
        }
    }

    private fun createIndependentParticleEffect(
        position: Vector3,
        particleEffectDefinition: ParticleEffectDefinition,
        ttlInSeconds: Int = MathUtils.random(10, 20)
    ) {
        gamePlayManagers.ecs.entityBuilder.begin().addParticleEffectComponent(
            position,
            gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(
                particleEffectDefinition
            ), ttlInSeconds = ttlInSeconds
        ).finishAndAddToEngine()
    }

    private fun destroyTree(
        tree: Entity,
        decorateWithSmokeAndFire: Boolean
    ) {
        val position =
            ComponentsMapper.modelInstance.get(tree).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector4
            )
        blowAmbToParts(
            auxVector3.set(position),
            ModelDefinition.PALM_TREE_LEAF,
            1,
            5,
            0.5F,
            decorateWithSmokeAndFire,
            0.125F,
            0.25F,
            AMB_PART_CREATION_POSITION_BIAS
        )
        blowAmbToParts(
            auxVector3.set(position),
            ModelDefinition.PALM_TREE_PART,
            1,
            2,
            1F,
            decorateWithSmokeAndFire,
            0.125F,
            0.25F,
            AMB_PART_CREATION_POSITION_BIAS
        )
        destroyAmbObject(tree)
        gamePlayManagers.soundPlayer.play(
            gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.TREE_FALL),
            position
        )
    }

    private fun destroyAmbObject(
        entity: Entity
    ) {
        val position =
            ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val ambComponent = ComponentsMapper.amb.get(entity)
        gamePlayManagers.stainsHandler.addBigHole(position)
        ambComponent.hp = 0
        if (ComponentsMapper.fence.has(entity)) {
            val fenceComponent = ComponentsMapper.fence.get(entity)
            val entityBuilder = gamePlayManagers.ecs.entityBuilder
            val gameModelInstanceFactory = gamePlayManagers.factories.gameModelInstanceFactory
            fenceComponent.left?.let {
                entityBuilder.addChildModelInstanceComponentToEntity(
                    it,
                    gameModelInstanceFactory.createGameModelInstance(ModelDefinition.FENCE_DESTROYED_RIGHT),
                    true,
                    auxVector1.set(0F, 0F, 0.95F)
                )
            }
            fenceComponent.right?.let {
                entityBuilder.addChildModelInstanceComponentToEntity(
                    it,
                    gameModelInstanceFactory.createGameModelInstance(ModelDefinition.FENCE_DESTROYED_LEFT),
                    true,
                    auxVector1.set(0F, 0F, -0.95F)
                )
            }
        }
        engine.removeEntity(entity)
    }

    @Suppress("SameParameterValue")
    private fun blowAmbToParts(
        position: Vector3,
        modelDefinition: ModelDefinition,
        min: Int,
        max: Int,
        gravityScale: Float,
        decorateWithSmokeAndFire: Boolean,
        minImpulse: Float,
        maxImpulse: Float,
        positionBiasMax: Float
    ) {
        val numberOfParts = MathUtils.random(min, max)
        for (i in 0 until numberOfParts) {
            val entity = createAmbPartEntity(modelDefinition, position, positionBiasMax)
            if (decorateWithSmokeAndFire && MathUtils.random() > 0.6F) {
                gamePlayManagers.ecs.entityBuilder.addParticleEffectComponentToEntity(
                    entity,
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(
                        if (MathUtils.randomBoolean()) FIRE_LOOP_SMALL else SMOKE_UP_LOOP
                    ), ttlInSeconds = MathUtils.random(10, 20), ttlForComponentOnly = true
                )
            }
            addPhysicsToAmbPart(
                entity,
                gravityScale,
                minImpulse,
                maxImpulse,
            )
        }
    }

    private fun createAmbPartEntity(
        modelDefinition: ModelDefinition,
        position: Vector3,
        positionBiasMax: Float
    ): Entity {
        val gameModelInstance = gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(
            modelDefinition,
        )
        val entity = gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                model = gameModelInstance,
                position = auxVector1.set(position).add(
                    MathUtils.random(-positionBiasMax, positionBiasMax),
                    MathUtils.random(0.1F, positionBiasMax),
                    MathUtils.random(-positionBiasMax, positionBiasMax)
                ),
                boundingBox = gamePlayManagers.assetsManager.getCachedBoundingBox(modelDefinition),
            )
            .addFlyingPartComponent()
            .finishAndAddToEngine()
        fadingAwayHandler.add(entity)
        gameModelInstance.modelInstance.transform.rotate(
            auxQuat.idt().setEulerAngles(
                MathUtils.random(0F, 360F),
                MathUtils.random(0F, 360F),
                MathUtils.random(0F, 360F)
            )
        )
        return entity
    }

    private fun addPhysicsToAmbPart(
        entity: Entity,
        gravityScale: Float,
        minImpulse: Float,
        maxImpulse: Float,
    ) {
        val gameModelInstance = ComponentsMapper.modelInstance.get(entity).gameModelInstance
        gamePlayManagers.ecs.entityBuilder.addPhysicsComponentPooledToEntity(
            entity,
            gameSessionData.gamePlayData.pools.rigidBodyPools.obtainRigidBodyPool(gameModelInstance.definition!!),
            CollisionFlags.CF_CHARACTER_OBJECT,
            gameModelInstance.modelInstance.transform,
            gravityScale
        ).rigidBody.applyImpulse(
            auxVector1.set(
                MathUtils.random(-1F, 1F),
                MathUtils.random(-1F, 1F),
                MathUtils.random(-1F, 1F)
            ).scl(MathUtils.random(minImpulse, maxImpulse)),
            auxVector2.set(
                MathUtils.random(-AMB_PART_IMPULSE_COMPONENT, AMB_PART_IMPULSE_COMPONENT),
                MathUtils.random(-AMB_PART_IMPULSE_COMPONENT, AMB_PART_IMPULSE_COMPONENT),
                MathUtils.random(-AMB_PART_IMPULSE_COMPONENT, AMB_PART_IMPULSE_COMPONENT)
            )
        )
    }

    private fun handleCollisionGroundWithHeavyStuffOnHighSpeed(entity0: Entity, entity1: Entity): Boolean {
        val roadComponent = ComponentsMapper.road.get(entity0)
        val physicsComponent = ComponentsMapper.physics.get(entity1)
        val result = false
        val ambCorpsePart = ComponentsMapper.ambCorpsePart.get(entity1)
        if (ComponentsMapper.ground.has(entity0) && ambCorpsePart != null) {
            val rigidBody = physicsComponent.rigidBody
            if (rigidBody.linearVelocity.len2() > 4 && rigidBody.mass > 5 || ambCorpsePart.destroyOnGroundImpact) {
                if (ambCorpsePart.collisionSound != null) {
                    gamePlayManagers.soundPlayer.play(
                        gamePlayManagers.assetsManager.getAssetByDefinition(ambCorpsePart.collisionSound),
                        rigidBody.worldTransform.getTranslation(auxVector1)
                    )
                }
                applyAmbCorpsePartDestructionOnGroundImpact(ambCorpsePart, rigidBody, entity1)
            }
            return result
        }
        if (roadComponent != null && physicsComponent != null) {
            val rigidBody = physicsComponent.rigidBody
            if (rigidBody.linearVelocity.len2() > 4 && rigidBody.mass > 5) {
                handleRoadHit(entity0, true)
            }
            return result
        }
        return result
    }

    private fun applyAmbCorpsePartDestructionOnGroundImpact(
        ambCorpsePart: AmbCorpsePart,
        rigidBody: RigidBody,
        entity: Entity
    ) {
        if (ambCorpsePart.destroyOnGroundImpact && !(entity.isRemoving || entity.isScheduledForRemoval)) {
            val specialEffectsFactory = gamePlayManagers.factories.specialEffectsFactory
            specialEffectsFactory.generateExplosion(
                rigidBody.worldTransform.getTranslation(auxVector1), blastRing = true, playSound = true
            )
            specialEffectsFactory.generateExplosion(
                rigidBody.worldTransform.getTranslation(auxVector1),
                addBiasToPosition = true,
                blastRing = false,
                playSound = false
            )
            specialEffectsFactory.generateExplosion(
                rigidBody.worldTransform.getTranslation(auxVector1),
                addBiasToPosition = true,
                blastRing = false,
                playSound = false
            )
            specialEffectsFactory.generateFlyingParts(entity, ModelDefinition.WATCH_TOWER_DESTROYED_PART, 2, 3, 3F)
            engine.removeEntity(entity)
        }
    }

    private fun handleCollisionRoadWithBullets(entity0: Entity, entity1: Entity): Boolean {
        val roadComponent = ComponentsMapper.road.get(entity0)
        val bulletComponent = ComponentsMapper.bullet.get(entity1)
        if (roadComponent != null && bulletComponent != null) {
            if (bulletComponent.explosive) {
                handleRoadHit(entity0)
            }
            return true
        }
        return false
    }

    private fun handleRoadHit(
        roadEntity: Entity,
        forceDestruction: Boolean = false
    ) {
        val roadComponent = ComponentsMapper.road.get(roadEntity)
        if (roadComponent.hp > 0) {
            roadComponent.takeDamage()
            if (roadComponent.hp <= 0 || forceDestruction) {
                val modelInstance = ComponentsMapper.modelInstance.get(roadEntity).gameModelInstance.modelInstance
                val position =
                    modelInstance.transform.getTranslation(
                        auxVector1
                    )
                val assetsManager = gamePlayManagers.assetsManager
                gamePlayManagers.soundPlayer.play(
                    assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION_SMALL),
                    position
                )
                gamePlayManagers.factories.specialEffectsFactory.generateSmallFlyingParts(position.add(0F, 0.2F, 0F))
                val attribute = modelInstance.materials.get(0).get(TextureAttribute.Diffuse) as TextureAttribute
                val name = "${roadComponent.textureDefinition.fileName}_dead"
                gamePlayManagers.assetsManager.getTexturesDefinitions().definitions[name]?.let {
                    attribute.textureDescription.texture =
                        assetsManager.getTexture(it, MathUtils.random(1, it.frames) - 1)
                }
            }
        }
    }

    private fun findBase(entity: Entity): Entity {
        val characterColor = ComponentsMapper.character.get(entity).color
        return bases.find { ComponentsMapper.base.get(it).color == characterColor }!!
    }

    private fun closeDoors(base: Entity, gamePlayManagers: GamePlayManagers) {
        val baseComponent = ComponentsMapper.base.get(base)
        baseComponent.close()
        baseComponent.baseDoorSoundId =
            gamePlayManagers.soundPlayer.play(
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_MOVE),
                ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                )
            )
    }

    override fun initialize(gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        super.initialize(gameSessionData, gamePlayManagers)
        val tilesMapping = gameSessionData.mapData.currentMap.tilesMapping
        waterSplashHandler.init(gameSessionData)
        gameSessionData.mapData.tilesEntities =
            Array(tilesMapping.size) { arrayOfNulls(tilesMapping[0].size) }
        gameSessionData.renderData.floorModel = createFloorModel()
        gameSessionData.renderData.modelCache = ModelCache()
    }


    override fun onSystemReady() {
        super.onSystemReady()
        MapInflater(gameSessionData, gamePlayManagers, engine).inflate()
        bases.forEach {
            initializeBase(it)
        }
        gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.MAP_SYSTEM_READY.ordinal)
    }

    private fun initializeBase(base: Entity) {
        addStage(base)
        val baseComponent = ComponentsMapper.base.get(base)
        baseComponent.init(
            addBaseDoor(base, 0F, -1F),
            addBaseDoor(base, 180F, 1F)
        )
        val sourcePosition =
            ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )

        baseComponent.baseDoorSoundId =
            gamePlayManagers.soundPlayer.play(
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_MOVE),
                sourcePosition
            )
    }

    private fun addBaseDoor(base: Entity, rotationAroundY: Float, relativeTargetX: Float): Entity {
        val doorModelInstance = GameModelInstance(
            ModelInstance(gamePlayManagers.assetsManager.getAssetByDefinition(ModelDefinition.PIT_DOOR)),
            ModelDefinition.PIT_DOOR
        )
        val basePosition =
            ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val door = gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                doorModelInstance,
                basePosition.add(1F, -0.1F, 1F), null
            )
            .addBaseDoorComponent(basePosition.x, basePosition.x + relativeTargetX)
            .finishAndAddToEngine()
        doorModelInstance.modelInstance.transform.rotate(Vector3.Y, rotationAroundY)
        val baseComponent = ComponentsMapper.base.get(base)
        val color =
            if (baseComponent.color == CharacterColor.BROWN) "pit_door_texture_brown" else "pit_door_texture_green"
        val textureAttribute =
            ComponentsMapper.modelInstance.get(door).gameModelInstance.modelInstance.materials.get(0)
                .get(TextureAttribute.Diffuse) as TextureAttribute
        textureAttribute.textureDescription.texture =
            gamePlayManagers.assetsManager.getTexture(color)

        return door
    }

    private fun addStage(base: Entity): Entity {
        val color =
            if (ComponentsMapper.base.get(base).color == CharacterColor.BROWN) "stage_texture_brown" else "stage_texture_green"
        val texture =
            gamePlayManagers.assetsManager.getTexture(color)
        return gamePlayManagers.ecs.entityBuilder.begin()
            .addModelInstanceComponent(
                model = GameModelInstance(
                    ModelInstance(gamePlayManagers.assetsManager.getAssetByDefinition(ModelDefinition.STAGE)),
                    ModelDefinition.STAGE
                ),
                position = ComponentsMapper.modelInstance.get(base).gameModelInstance.modelInstance.transform.getTranslation(
                    auxVector1
                ).add(1F, StageComponent.BOTTOM_EDGE_Y, 1F),
                boundingBox = null,
                texture = texture
            )
            .addChildDecalComponent(
                listOf(landingMark), false
            )
            .addStageComponent(base)
            .finishAndAddToEngine()
    }

    private fun createLandingMark(): ChildDecal {
        val definition = gamePlayManagers.assetsManager.getTexturesDefinitions().definitions["landing_mark"]
        val landingMarkFrame0 = TextureRegion(gamePlayManagers.assetsManager.getTexture(definition!!, 0))
        val landingMarkFrame1 = TextureRegion(gamePlayManagers.assetsManager.getTexture(definition, 1))
        val decal = Decal.newDecal(2F, 2F, TextureRegion(landingMarkFrame0), true)
        decal.setColor(decal.color.r, decal.color.g, decal.color.b, 0.5F)
        val frames = com.badlogic.gdx.utils.Array<TextureRegion>()
        frames.add(landingMarkFrame0)
        frames.add(landingMarkFrame1)
        return ChildDecal(
            decal,
            Vector3(0F, 1F, 0F),
            Quaternion().setEulerAngles(0F, 90F, 0F),
            DecalAnimation(1F, frames)
        )

    }


    override fun resume(delta: Long) {
        ambSoundsHandler.resume(delta)
    }

    override fun update(deltaTime: Float) {
        super.update(deltaTime)
        for (base in bases) {
            updateBaseDoors(base, deltaTime)
        }
        ambSoundsHandler.update(gamePlayManagers)
        groundTextureAnimationHandler.update(deltaTime)
        waterSplashHandler.update(deltaTime)
        fadingAwayHandler.update(deltaTime)
        val treeEntities = mapSystemRelatedEntities.treeEntities
        for (tree in treeEntities) {
            val physicsComponent = ComponentsMapper.physics.get(tree)
            val rigidBody = physicsComponent.rigidBody
            if (rigidBody.collisionFlags == CollisionFlags.CF_CHARACTER_OBJECT) {
                rigidBody.getWorldTransform(auxMatrix)
                auxMatrix.getRotation(auxQuat)
                val upVector = auxVector2.set(0f, 1f, 0f)
                auxQuat.transform(upVector)
                if (upVector.y < 0.85f) {
                    destroyTree(tree, false)
                }
            }
        }
        updateFallingBuildings()
    }

    private fun updateFallingBuildings() {
        for (fallingBuilding in fallingBuildings) {
            val physicsComponent = ComponentsMapper.physics.get(fallingBuilding)
            if (physicsComponent.rigidBody.worldTransform.getTranslation(auxVector1).y < -ComponentsMapper.amb.get(
                    fallingBuilding
                ).def.collapseThreshold
            ) {
                fallingBuildingAnimationDone(fallingBuilding)
            }
        }
        for (fallingBuilding in fallingBuildingsToRemove) {
            fallingBuildings.remove(fallingBuilding)
        }
        fallingBuildingsToRemove.clear()
    }

    private fun fallingBuildingAnimationDone(fallingBuilding: Entity) {
        val corpse = ComponentsMapper.amb.get(fallingBuilding).def.corpse ?: return

        auxMatrix.set(ComponentsMapper.modelInstance.get(fallingBuilding).gameModelInstance.modelInstance.transform)
        destroyAmbObject(fallingBuilding)
        val gameModelInstance = gamePlayManagers.factories.gameModelInstanceFactory.createGameModelInstance(
            corpse
        )
        val position = auxMatrix.getTranslation(auxVector4)
        auxMatrix.setTranslation(position.x, 0F, position.z)
        val entityBuilder = gamePlayManagers.ecs.entityBuilder
        val destroyedBuilding = entityBuilder.begin().addModelInstanceComponent(
            gameModelInstance,
            Vector3.Zero,
            gamePlayManagers.assetsManager.getCachedBoundingBox(ModelDefinition.BUILDING_0_DESTROYED)
        ).finishAndAddToEngine()
        val transform = gameModelInstance.modelInstance.transform.set(auxMatrix)
        entityBuilder.addPhysicsComponentToEntity(
            destroyedBuilding,
            ModelDefinition.BUILDING_0_DESTROYED.physicalShapeCreator!!.create(),
            0F,
            CollisionFlags.CF_STATIC_OBJECT,
            transform
        )
        fallingBuildingsToRemove.add(fallingBuilding)
        val specialEffectsFactory = gamePlayManagers.factories.specialEffectsFactory
        val sideBias = 1.5F
        val height = 0.5F
        auxMatrix.getTranslation(position)
        specialEffectsFactory.generateExplosion(
            auxVector2.set(position).add(-sideBias, height, sideBias),
            true,
            playSound = false
        )
        specialEffectsFactory.generateExplosion(
            auxVector2.set(position).add(sideBias, height, sideBias),
            true,
            playSound = false
        )
        specialEffectsFactory.generateExplosion(
            auxVector2.set(position).add(sideBias, height, -sideBias),
            true,
            playSound = false
        )
        specialEffectsFactory.generateExplosion(
            auxVector2.set(position).add(-sideBias, height, -sideBias),
            true,
            playSound = false
        )
        specialEffectsFactory.generateExplosion(
            auxVector2.set(position).add(-sideBias, height, 0F),
            true,
            playSound = false
        )
        specialEffectsFactory.generateExplosion(
            auxVector2.set(position).add(sideBias, height, 0F),
            true,
            playSound = false
        )
        gamePlayManagers.soundPlayer.play(
            gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION_HUGE),
            position
        )
        blowAmbToParts(
            position,
            ModelDefinition.BUILDING_0_PART,
            2, 3,
            0.5F,
            true,
            2F, 4F, 1.5F
        )
    }

    private fun updateBaseDoors(base: Entity, deltaTime: Float) {
        val baseComponent = ComponentsMapper.base.get(base)
        if (baseComponent.isIdle()) return

        val westDoorTransform =
            ComponentsMapper.modelInstance.get(baseComponent.westDoor).gameModelInstance.modelInstance.transform
        val eastDoorTransform =
            ComponentsMapper.modelInstance.get(baseComponent.eastDoor).gameModelInstance.modelInstance.transform
        val stepSize = deltaTime * baseComponent.doorMoveState
        val westDoorBaseDoorComponent = ComponentsMapper.baseDoor.get(baseComponent.westDoor)
        val eastDoorBaseDoorComponent = ComponentsMapper.baseDoor.get(baseComponent.eastDoor)
        updateWestDoor(baseComponent, westDoorBaseDoorComponent, westDoorTransform, stepSize)
        updateEastDoor(baseComponent, eastDoorBaseDoorComponent, eastDoorTransform, stepSize)
    }

    private fun updateEastDoor(
        baseComponent: BaseComponent,
        eastDoorBaseDoorComponent: BaseDoorComponent,
        eastDoorTransform: Matrix4,
        stepSize: Float,
    ) {
        if (TimeUtils.timeSinceMillis(baseComponent.latestCloseTime) < DOORS_DELAY) return

        val eastDoorX = eastDoorTransform.getTranslation(auxVector2).x
        val isOpening = baseComponent.doorMoveState > 0
        val isClosing = baseComponent.doorMoveState < 0
        if ((isOpening && eastDoorX < eastDoorBaseDoorComponent.targetX)
            || (isClosing && eastDoorX > eastDoorBaseDoorComponent.initialX)
        ) {
            eastDoorTransform.trn(stepSize, 0F, 0F).getTranslation(auxVector1)
            auxVector1.x = max(eastDoorBaseDoorComponent.initialX, auxVector1.x)
            auxVector1.x = min(eastDoorBaseDoorComponent.targetX, auxVector1.x)
            eastDoorTransform.setTranslation(auxVector1)
        } else {
            baseComponent.setIdle()
            gamePlayManagers.soundPlayer.stop(
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_MOVE),
                baseComponent.baseDoorSoundId
            )
            gamePlayManagers.soundPlayer.play(
                gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.BASE_DOOR_DONE),
                auxVector2
            )
        }
    }

    private fun updateWestDoor(
        baseComponent: BaseComponent,
        westDoorBaseDoorComponent: BaseDoorComponent,
        westDoorTransform: Matrix4,
        stepSize: Float
    ) {
        if (TimeUtils.timeSinceMillis(baseComponent.latestCloseTime) < DOORS_DELAY) return

        val isOpening = baseComponent.doorMoveState > 0
        val isClosing = baseComponent.doorMoveState < 0
        val westDoorX = westDoorTransform.getTranslation(auxVector1).x
        if ((isOpening && westDoorX > westDoorBaseDoorComponent.targetX)
            || (isClosing && westDoorX < westDoorBaseDoorComponent.initialX)
        ) {
            westDoorTransform.trn(-stepSize, 0F, 0F).getTranslation(auxVector1)
            auxVector1.x = max(westDoorBaseDoorComponent.targetX, auxVector1.x)
            auxVector1.x = min(westDoorBaseDoorComponent.initialX, auxVector1.x)
            westDoorTransform.setTranslation(auxVector1)
        }
    }


    override fun dispose() {
        gameSessionData.renderData.modelCache.dispose()
    }


    private fun createFloorModel(): Model {
        val builder = ModelBuilder()
        builder.begin()
        val texture =
            gamePlayManagers.assetsManager.getTexture("tile_water")
        GeneralUtils.createFlatMesh(builder, "floor", 0.5F, texture, 0F)
        return builder.end()
    }

    companion object {
        private const val AMB_PART_CREATION_POSITION_BIAS = 0.05F
        private const val AMB_PART_IMPULSE_COMPONENT = 0.1F
        const val DOORS_DELAY = 1000F
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxVector4 = Vector3()
        private val auxMatrix = Matrix4()
        private val auxQuat = Quaternion()
        private val auxBoundingBox = BoundingBox()
    }
}
