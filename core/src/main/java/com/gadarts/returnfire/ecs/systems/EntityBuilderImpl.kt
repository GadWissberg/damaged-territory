package com.gadarts.returnfire.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.CollisionConstants.DISABLE_DEACTIVATION
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.ecs.components.*
import com.gadarts.returnfire.ecs.components.ai.*
import com.gadarts.returnfire.ecs.components.amb.AmbAnimationComponent
import com.gadarts.returnfire.ecs.components.amb.AmbComponent
import com.gadarts.returnfire.ecs.components.amb.AmbCorpsePart
import com.gadarts.returnfire.ecs.components.amb.AmbSoundComponent
import com.gadarts.returnfire.ecs.components.arm.ArmComponent
import com.gadarts.returnfire.ecs.components.arm.ArmProperties
import com.gadarts.returnfire.ecs.components.arm.PrimaryArmComponent
import com.gadarts.returnfire.ecs.components.arm.SecondaryArmComponent
import com.gadarts.returnfire.ecs.components.bullet.BulletBehavior
import com.gadarts.returnfire.ecs.components.bullet.BulletComponent
import com.gadarts.returnfire.ecs.components.cd.ChildDecal
import com.gadarts.returnfire.ecs.components.cd.ChildDecalComponent
import com.gadarts.returnfire.ecs.components.effects.*
import com.gadarts.returnfire.ecs.components.model.GameModelInstance
import com.gadarts.returnfire.ecs.components.model.ModelInstanceComponent
import com.gadarts.returnfire.ecs.components.onboarding.BoardingAnimation
import com.gadarts.returnfire.ecs.components.onboarding.BoardingComponent
import com.gadarts.returnfire.ecs.components.physics.GhostPhysicsComponent
import com.gadarts.returnfire.ecs.components.physics.MotionState
import com.gadarts.returnfire.ecs.components.physics.PhysicsComponent
import com.gadarts.returnfire.ecs.components.physics.RigidBody
import com.gadarts.returnfire.ecs.components.pit.ElevatorDoorComponent
import com.gadarts.returnfire.ecs.components.pit.HangarComponent
import com.gadarts.returnfire.ecs.components.turret.TurretBaseComponent
import com.gadarts.returnfire.ecs.components.turret.TurretCannonComponent
import com.gadarts.returnfire.ecs.components.turret.TurretComponent
import com.gadarts.returnfire.ecs.systems.data.pools.GameParticleEffectPool
import com.gadarts.returnfire.ecs.systems.data.pools.RigidBodyPool
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.factories.Factories
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition
import com.gadarts.shared.assets.definitions.SoundDefinition
import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.AmbDefinition
import com.gadarts.shared.data.definitions.characters.CharacterDefinition

class EntityBuilderImpl : EntityBuilder {
    private lateinit var messageDispatcher: MessageDispatcher
    private lateinit var factories: Factories
    private lateinit var engine: PooledEngine

    fun init(engine: PooledEngine, factories: Factories, messageDispatcher: MessageDispatcher) {
        this.engine = engine
        this.factories = factories
        this.messageDispatcher = messageDispatcher
    }

    override fun begin(): EntityBuilderImpl {
        entity = engine.createEntity()
        return this
    }

    override fun addModelInstanceComponent(
        model: GameModelInstance,
        position: Vector3,
        boundingBox: BoundingBox?,
        direction: Float,
        hidden: Boolean,
        texture: Texture?,
        haloEffect: Boolean
    ): EntityBuilderImpl {
        addModelInstanceComponent(entity!!, model, position, boundingBox, direction, hidden, texture, haloEffect)
        return this
    }

    private fun addModelInstanceComponent(
        entity: Entity,
        model: GameModelInstance,
        position: Vector3,
        boundingBox: BoundingBox?,
        direction: Float,
        hidden: Boolean,
        texture: Texture?,
        haloEffect: Boolean
    ) {
        val modelInstanceComponent = engine.createComponent(ModelInstanceComponent::class.java)
        modelInstanceComponent.init(
            gameModelInstance = model,
            position = position,
            boundingBox = boundingBox,
            direction = direction,
            hidden = hidden,
            texture = texture,
            haloEffect = if (haloEffect) factories.specialEffectsFactory.generateHaloEffect() else null,
        )
        entity.add(modelInstanceComponent)
    }

    override fun addModelInstanceComponentToEntity(
        entity: Entity,
        model: GameModelInstance,
        position: Vector3,
        boundingBox: BoundingBox?,
        direction: Float,
        hidden: Boolean,
        texture: Texture?,
        haloEffect: Boolean
    ): EntityBuilder {
        addModelInstanceComponent(entity, model, position, boundingBox, direction, hidden, texture, haloEffect)
        return this
    }


    override fun finishAndAddToEngine(): Entity {
        engine.addEntity(entity)
        val result = entity
        entity = null
        return result!!
    }

    override fun addChildDecalComponent(
        decals: List<ChildDecal>,
        visible: Boolean,
    ): EntityBuilder {
        val component = ChildDecalComponent(decals, visible)
        entity!!.add(component)
        return this
    }

    override fun addAmbSoundComponent(sound: Sound): EntityBuilder {
        addAmbSoundComponent(entity!!, sound)
        return this
    }

    override fun addAmbSoundComponentToEntity(entity: Entity, sound: Sound): EntityBuilder {
        addAmbSoundComponent(entity, sound)
        return this
    }

    @Suppress("KotlinConstantConditions")
    override fun addCharacterComponent(characterDefinition: CharacterDefinition, color: CharacterColor): EntityBuilder {
        val characterComponent = CharacterComponent(
            characterDefinition,
            color,
            if (GameDebugSettings.FORCE_ENEMY_HP >= 0 && color == CharacterColor.GREEN) GameDebugSettings.FORCE_ENEMY_HP else characterDefinition.getHP()
        )
        entity!!.add(characterComponent)
        return this
    }

    override fun addBoardingCharacterComponent(
        color: CharacterColor,
        boardingAnimation: BoardingAnimation?
    ): EntityBuilder {
        val boardingComponent = BoardingComponent(color, boardingAnimation)
        entity!!.add(boardingComponent)
        return this
    }

    override fun addPlayerComponent(): EntityBuilder {
        addPlayerComponentToEntity(entity!!)
        return this
    }

    override fun addPlayerComponentToEntity(entity: Entity) {
        val playerComponent = PlayerComponent()
        entity.add(playerComponent)
    }

    override fun addPrimaryArmComponent(
        spark: Entity,
        armProperties: ArmProperties,
        bulletBehavior: BulletBehavior
    ): EntityBuilder {
        ComponentsMapper.spark.get(spark).parent = entity!!
        val armComponent = PrimaryArmComponent(armProperties, spark, bulletBehavior)
        entity!!.add(armComponent)
        return this
    }


    override fun addSecondaryArmComponent(
        spark: Entity,
        armProperties: ArmProperties,
        bulletBehavior: BulletBehavior
    ): EntityBuilder {
        ComponentsMapper.spark.get(spark).parent = entity!!
        val armComponent = SecondaryArmComponent(armProperties, spark, bulletBehavior)
        entity!!.add(armComponent)
        return this
    }

    override fun addBulletComponent(
        bulletBehavior: BulletBehavior,
        explosion: ParticleEffectDefinition?,
        explosive: Boolean,
        friendly: Boolean,
        damage: Float,
        destroyOnSky: Boolean
    ): EntityBuilderImpl {
        val bulletComponent = engine.createComponent(BulletComponent::class.java)
        bulletComponent.init(bulletBehavior, explosion, explosive, friendly, damage, destroyOnSky)
        entity!!.add(bulletComponent)
        return this
    }

    override fun addAmbComponent(rotation: Float, def: AmbDefinition, scale: Vector3): EntityBuilderImpl {
        val ambComponent = AmbComponent(rotation, def, scale)
        entity!!.add(ambComponent)
        return this
    }


    override fun addGroundComponent(): EntityBuilderImpl {
        val groundComponent = engine.createComponent(GroundComponent::class.java)
        entity!!.add(groundComponent)
        return this
    }

    override fun addParticleEffectComponent(
        position: Vector3,
        pool: GameParticleEffectPool,
        rotationAroundY: Float,
        followRelativePosition: Vector3,
        ttlInSeconds: Int,
        ttlForComponentOnly: Boolean,
        followSpecificEntity: Entity?
    ): EntityBuilderImpl {
        val particleEffectComponent = createParticleEffectComponent(
            pool,
            rotationAroundY,
            position,
            ttlInSeconds,
            ttlForComponentOnly,
            followRelativePosition,
            followSpecificEntity ?: entity,
        )
        entity!!.add(particleEffectComponent)
        return this
    }

    override fun addParticleEffectComponentToEntity(
        entity: Entity,
        pool: GameParticleEffectPool,
        followRelativePosition: Vector3,
        ttlInSeconds: Int,
        ttlForComponentOnly: Boolean
    ) {
        val component = createParticleEffectComponent(
            pool = pool,
            ttlInSeconds = ttlInSeconds,
            followRelativePosition = followRelativePosition,
            followEntity = entity,
            ttlForComponentOnly = ttlForComponentOnly,
        )
        entity.add(component)
        messageDispatcher.dispatchMessage(
            SystemEvents.PARTICLE_EFFECTS_COMPONENTS_ADDED_MANUALLY.ordinal,
            entity
        )
    }

    override fun addSparkComponent(
        relativePositionCalculator: ArmComponent.RelativePositionCalculator,
    ): EntityBuilderImpl {
        val sparkComponent = SparkComponent(relativePositionCalculator)
        entity!!.add(sparkComponent)
        return this
    }

    override fun addGroundBlastComponent(scalePace: Float, duration: Int, fadeOutPace: Float): EntityBuilder {
        val groundBlastComponent = engine.createComponent(GroundBlastComponent::class.java)
        groundBlastComponent.init(scalePace, duration, fadeOutPace)
        entity!!.add(groundBlastComponent)
        return this
    }

    override fun addTurretComponent(
        base: Entity,
        followBasePosition: Boolean,
        followBaseRotation: Boolean,
        relativeY: Float,
        cannon: Entity?
    ): EntityBuilder {
        val turretComponent = TurretComponent(base, followBasePosition, followBaseRotation, relativeY, cannon)
        entity!!.add(turretComponent)
        return this
    }

    override fun addBaseAiComponent(initialHp: Float, target: Entity?): EntityBuilder {
        addBaseAiComponentToEntity(entity!!, initialHp, target)
        return this
    }

    override fun addBaseAiComponentToEntity(entity: Entity, initialHp: Float, target: Entity?): BaseAiComponent {
        val baseAiComponent = BaseAiComponent(target)
        entity.add(baseAiComponent)
        return baseAiComponent
    }

    override fun addElevatorComponent(base: Entity): EntityBuilder {
        val elevatorComponent = ElevatorComponent(base)
        entity!!.add(elevatorComponent)
        return this
    }

    override fun addAutoAimComponent(): EntityBuilder {
        val autoAimComponent = AutoAimComponent()
        entity!!.add(autoAimComponent)
        return this
    }

    override fun addHangarComponent(color: CharacterColor): EntityBuilder {
        val hangarComponent = HangarComponent(color)
        entity!!.add(hangarComponent)
        return this
    }

    override fun addCrashSoundEmitterComponent(soundToStop: Sound, soundToStopId: Long): EntityBuilder {
        addCrashSoundEmitterComponentToEntity(entity!!, soundToStop, soundToStopId)
        return this
    }

    override fun addCrashSoundEmitterComponentToEntity(
        entity: Entity,
        soundToStop: Sound,
        soundToStopId: Long
    ): EntityBuilder {
        val crashingAircraftEmitter = CrashingAircraftEmitter(soundToStop, soundToStopId)
        entity.add(crashingAircraftEmitter)
        return this
    }

    override fun addLimitedVelocityComponent(maxValue: Float): EntityBuilder {
        val limitedVelocityComponent = LimitedVelocityComponent(maxValue)
        entity!!.add(limitedVelocityComponent)
        return this
    }

    override fun addRoadComponentToEntity(entity: Entity, textureDefinition: TextureDefinition): EntityBuilder {
        val roadComponent = RoadComponent(textureDefinition)
        entity.add(roadComponent)
        return this
    }

    override fun addModelCacheComponentToEntity(tileEntity: Entity) {
        val modelCacheComponent = ModelCacheComponent()
        tileEntity.add(modelCacheComponent)
    }

    override fun addAnimationComponentToEntity(
        entity: Entity,
        loop: Boolean,
        modelInstance: ModelInstance
    ): AmbAnimationComponent {
        val ambAnimationComponent = AmbAnimationComponent(loop, modelInstance)
        entity.add(ambAnimationComponent)
        return ambAnimationComponent
    }

    override fun addBaseDoorComponent(initialX: Float, targetX: Float): EntityBuilderImpl {
        val elevatorDoorComponent = ElevatorDoorComponent(initialX, targetX)
        entity!!.add(elevatorDoorComponent)
        return this
    }

    override fun addTurretBaseComponent(): EntityBuilder {
        val turretComponent = TurretBaseComponent()
        entity!!.add(turretComponent)
        return this
    }

    override fun addChildModelInstanceComponent(
        gameModelInstance: GameModelInstance,
        followParentRotation: Boolean,
        relativePosition: Vector3
    ): EntityBuilder {
        addChildModelInstanceComponentToEntity(
            entity!!,
            gameModelInstance,
            followParentRotation,
            relativePosition
        )
        return this
    }

    override fun addChildModelInstanceComponentToEntity(
        entity: Entity,
        gameModelInstance: GameModelInstance,
        followParentRotation: Boolean,
        relativePosition: Vector3
    ): ChildModelInstanceComponent {
        val childModelInstanceComponent =
            ChildModelInstanceComponent(gameModelInstance, followParentRotation, relativePosition)
        entity.add(childModelInstanceComponent)
        return childModelInstanceComponent
    }

    override fun addPhysicsComponent(
        shape: btCollisionShape,
        collisionFlag: Int,
        transform: Matrix4,
        gravityScalar: Float,
        mass: Float
    ): EntityBuilderImpl {
        val physicsComponent = addPhysicsComponentToEntity(
            entity!!,
            shape,
            mass,
            collisionFlag,
            transform,
            gravityScalar,
        )
        entity!!.add(physicsComponent)
        return this
    }

    override fun finish(): Entity {
        val result = entity
        entity = null
        return result!!
    }

    override fun addPhysicsComponentPooledToEntity(
        entity: Entity,
        rigidBodyPool: RigidBodyPool,
        collisionFlag: Int,
        transform: Matrix4,
        gravityScalar: Float
    ): PhysicsComponent {
        return addPhysicsComponent(
            entity,
            rigidBodyPool.obtain(),
            collisionFlag,
            transform,
            gravityScalar
        )
    }

    override fun addPhysicsComponentToEntity(
        entity: Entity,
        shape: btCollisionShape,
        mass: Float,
        collisionFlag: Int,
        transform: Matrix4,
        gravityScalar: Float,
        friction: Float,
        activationState: Int
    ): PhysicsComponent {
        val rigidBody = factories.rigidBodyFactory.create(mass, shape, null, transform)
        return addPhysicsComponent(
            entity,
            rigidBody,
            collisionFlag,
            transform,
            gravityScalar,
            friction,
            activationState
        )
    }

    override fun addGhostPhysicsComponentToEntity(
        entity: Entity,
        shape: btCollisionShape,
        position: Vector3,
    ): GhostPhysicsComponent {
        val ghost = factories.ghostFactory.create(shape, position)
        val ghostPhysicsComponent = GhostPhysicsComponent(ghost)
        entity.add(ghostPhysicsComponent)
        ghost.userData = entity
        messageDispatcher.dispatchMessage(
            SystemEvents.PHYSICS_GHOST_COMPONENT_ADDED_MANUALLY.ordinal,
            entity
        )
        return ghostPhysicsComponent
    }

    override fun addFlyingPartComponent(): EntityBuilder {
        val flyingPartComponent = FlyingPartComponent()
        entity!!.add(flyingPartComponent)
        return this
    }

    override fun addTreeComponentToEntity(entity: Entity): TreeComponent {
        val treeComponent = TreeComponent()
        entity.add(treeComponent)
        return treeComponent
    }

    override fun addDeathSequenceComponent(): EntityBuilder {
        addDeathSequenceComponentToEntity(entity!!, minExplosions = 2, maxExplosions = 4)
        return this
    }

    override fun addDeathSequenceComponentToEntity(
        entity: Entity,
        createExplosionsAround: Boolean,
        minExplosions: Int,
        maxExplosions: Int
    ): DeathSequenceComponent {
        val deathSequenceComponent = DeathSequenceComponent(createExplosionsAround, minExplosions, maxExplosions)
        entity.add(deathSequenceComponent)
        return deathSequenceComponent
    }

    override fun addAmbCorpsePart(
        destroyOnGroundImpact: Boolean,
        collisionSound: SoundDefinition?,
    ): EntityBuilder {
        val ambDeathPart = AmbCorpsePart(collisionSound, destroyOnGroundImpact)
        entity!!.add(ambDeathPart)
        return this
    }

    override fun addFenceComponentToEntity(entity: Entity): FenceComponent {
        val fenceComponent = FenceComponent()
        entity.add(fenceComponent)
        return fenceComponent
    }

    override fun addAiTurretComponentToEntity(turret: Entity): AiTurretComponent {
        val aiTurretComponent = AiTurretComponent()
        turret.add(aiTurretComponent)
        return aiTurretComponent
    }

    override fun addDrowningEffectComponent(): EntityBuilder {
        val drowningEffectComponent = DrowningEffectComponent()
        entity!!.add(drowningEffectComponent)
        return this
    }

    override fun addGroundCharacterAiComponentToEntity(entity: Entity): GroundCharacterAiComponent {
        val groundCharacterAiComponent = GroundCharacterAiComponent()
        entity.add(groundCharacterAiComponent)
        return groundCharacterAiComponent
    }

    override fun addApacheAiComponentToEntity(entity: Entity, initialHp: Float): ApacheAiComponent {
        val apacheAiComponent = ApacheAiComponent(initialHp)
        entity.add(apacheAiComponent)
        return apacheAiComponent
    }

    override fun addTurretEnemyAiComponent(): EntityBuilder {
        val turretEnemyAiComponent = TurretEnemyAiComponent()
        entity!!.add(turretEnemyAiComponent)
        return this
    }

    override fun addFrontWheelsComponent(wheel: ModelDefinition): EntityBuilder {
        val rightWheel = factories.gameModelInstanceFactory.createGameModelInstance(wheel)
        val leftWheel = factories.gameModelInstanceFactory.createGameModelInstance(wheel)
        val frontWheelsComponent = FrontWheelsComponent(rightWheel, leftWheel, 0.3F, 0.05F, 0.1F)
        entity!!.add(frontWheelsComponent)
        return this
    }

    override fun addTurretCannonComponent(relativeX: Float, relativeY: Float): EntityBuilder {
        val turretCannonComponent = TurretCannonComponent(relativeX, relativeY)
        entity!!.add(turretCannonComponent)
        return this
    }

    override fun addBrownComponent(): EntityBuilder {
        val brownComponent = BrownComponent()
        entity!!.add(brownComponent)
        return this
    }

    override fun addGreenComponent(): EntityBuilder {
        val greenComponent = GreenComponent()
        entity!!.add(greenComponent)
        return this
    }

    override fun addTurretAutomationComponent(): EntityBuilder {
        val turretAutomationComponent = TurretAutomationComponent()
        entity!!.add(turretAutomationComponent)
        return this
    }

    override fun addFlagComponentToEntity(entity: Entity, color: CharacterColor): FlagComponent {
        val flagComponent = FlagComponent(color)
        entity.add(flagComponent)
        return flagComponent
    }

    override fun addFlagFloorComponentToEntity(
        entity: Entity,
        color: CharacterColor
    ): FlagFloorComponent {
        val flagFloorComponent = FlagFloorComponent(color)
        entity.add(flagFloorComponent)
        return flagFloorComponent
    }

    private fun createParticleEffectComponent(
        pool: GameParticleEffectPool,
        rotationAroundY: Float = 0F,
        position: Vector3? = null,
        ttlInSeconds: Int,
        ttlForComponentOnly: Boolean,
        followRelativePosition: Vector3,
        followEntity: Entity?,
    ): ParticleEffectComponent {
        val effect: ParticleEffect = pool.obtain()
        val particleEffectComponent = engine.createComponent(
            ParticleEffectComponent::class.java
        )
        particleEffectComponent.init(
            effect,
            pool.definition,
            ttlInSeconds,
            ttlForComponentOnly,
            followRelativePosition,
            followEntity,
        )
        if (position != null) {
            val controllers = effect.controllers
            for (i in 0 until controllers.size) {
                val transform = controllers[i].transform
                transform.idt()
                transform.setTranslation(position)
                if (rotationAroundY != 0F) {
                    transform.rotate(Vector3.Y, rotationAroundY)
                }
            }
        }
        return particleEffectComponent
    }

    private fun addPhysicsComponent(
        entity: Entity,
        rigidBody: RigidBody,
        collisionFlag: Int?,
        transform: Matrix4?,
        gravityScalar: Float,
        friction: Float = 1.5F,
        activationState: Int = DISABLE_DEACTIVATION
    ): PhysicsComponent {
        initializeRigidBody(rigidBody, activationState, friction, collisionFlag, transform)
        val physicsComponent = engine.createComponent(PhysicsComponent::class.java)
        physicsComponent.init(rigidBody)
        physicsComponent.rigidBody.userData = entity
        entity.add(physicsComponent)
        messageDispatcher.dispatchMessage(
            SystemEvents.PHYSICS_COMPONENT_ADDED_MANUALLY.ordinal,
            entity
        )
        physicsComponent.rigidBody.gravity =
            if (gravityScalar > 0F) auxVector.set(PhysicsComponent.worldGravity)
                .scl(gravityScalar) else auxVector.setZero()
        return physicsComponent
    }

    private fun initializeRigidBody(
        rigidBody: RigidBody,
        activationState: Int,
        friction: Float,
        collisionFlag: Int?,
        transform: Matrix4?
    ) {
        rigidBody.angularVelocity = Vector3.Zero
        rigidBody.clearForces()
        rigidBody.setSleepingThresholds(1f, 1f)
        rigidBody.deactivationTime = 5f
        rigidBody.activate()
        rigidBody.activationState = activationState
        rigidBody.angularFactor = Vector3(1F, 1F, 1F)
        rigidBody.friction = friction
        if (collisionFlag != null) {
            rigidBody.collisionFlags = rigidBody.collisionFlags or collisionFlag
        }
        if (transform != null) {
            val motionState = rigidBody.motionState as MotionState
            motionState.transformObject = transform
            motionState.setWorldTransform(transform)
            rigidBody.worldTransform = transform
        }
    }


    private fun addAmbSoundComponent(entity: Entity, sound: Sound): Entity {
        val ambSoundComponent = AmbSoundComponent(sound)
        entity.add(ambSoundComponent)
        messageDispatcher.dispatchMessage(
            SystemEvents.AMB_SOUND_COMPONENT_ADDED.ordinal,
            entity
        )
        return entity
    }

    companion object {
        private val auxVector = Vector3()
        var entity: Entity? = null
    }
}
