package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.Collision
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.gadarts.returnfire.Factories
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.components.*
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.arm.PrimaryArmComponent
import com.gadarts.returnfire.components.arm.SecondaryArmComponent
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.model.ModelInstanceComponent
import com.gadarts.returnfire.components.onboarding.BoardingAnimation
import com.gadarts.returnfire.components.onboarding.BoardingComponent
import com.gadarts.returnfire.components.physics.MotionState
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.components.physics.RigidBody
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.systems.data.pools.GameParticleEffectPool
import com.gadarts.returnfire.systems.data.pools.RigidBodyPool
import com.gadarts.returnfire.systems.events.SystemEvents

class EntityBuilderImpl : EntityBuilder {
    private lateinit var messageDispatcher: MessageDispatcher
    private lateinit var factories: Factories
    private lateinit var engine: PooledEngine

    override fun begin(): EntityBuilderImpl {
        entity = engine.createEntity()
        return this
    }

    override fun addModelInstanceComponent(
        model: GameModelInstance,
        position: Vector3,
        boundingBox: BoundingBox?,
        direction: Float,
        hidden: Boolean
    ): EntityBuilderImpl {
        val modelInstanceComponent = engine.createComponent(ModelInstanceComponent::class.java)
        modelInstanceComponent.init(model, position, boundingBox, direction, hidden)
        entity!!.add(modelInstanceComponent)
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

    override fun addCharacterComponent(characterDefinition: CharacterDefinition): EntityBuilder {
        val characterComponent = CharacterComponent(characterDefinition)
        entity!!.add(characterComponent)
        return this
    }

    override fun addOnboardingCharacterComponent(boardingAnimation: BoardingAnimation?): EntityBuilder {
        val boardingComponent = BoardingComponent(boardingAnimation)
        entity!!.add(boardingComponent)
        return this
    }

    override fun addPlayerComponent(): EntityBuilder {
        val characterComponent = PlayerComponent()
        entity!!.add(characterComponent)
        return this
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
        damage: Int
    ): EntityBuilderImpl {
        val bulletComponent = engine.createComponent(BulletComponent::class.java)
        bulletComponent.init(bulletBehavior, explosion, explosive, friendly, damage)
        entity!!.add(bulletComponent)
        return this
    }

    override fun addAmbComponent(scale: Vector3, rotation: Float): EntityBuilderImpl {
        val ambComponent = AmbComponent(scale, rotation)
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
        thisEntityAsParent: Boolean,
        parentRelativePosition: Vector3,
        ttlInSeconds: Int
    ): EntityBuilderImpl {
        val effect: ParticleEffect = pool.obtain()
        val particleEffectComponent = engine.createComponent(
            ParticleEffectComponent::class.java
        )
        particleEffectComponent.init(
            effect,
            pool.definition,
            if (thisEntityAsParent) entity else null,
            ttlInSeconds,
            parentRelativePosition,
        )
        val controllers = effect.controllers
        for (i in 0 until controllers.size) {
            val transform = controllers[i].transform
            transform.idt()
            transform.setTranslation(position)
            if (rotationAroundY != 0F) {
                transform.rotate(Vector3.Y, rotationAroundY)
            }
        }
        entity!!.add(particleEffectComponent)
        return this
    }

    override fun addSparkComponent(
        relativePositionCalculator: ArmComponent.RelativePositionCalculator,
    ): EntityBuilderImpl {
        val sparkComponent = SparkComponent(relativePositionCalculator)
        entity!!.add(sparkComponent)
        return this
    }

    override fun addGroundBlastComponent(scalePace: Float, duration: Int, fadeOutPace: Float): EntityBuilderImpl {
        val groundBlastComponent = engine.createComponent(GroundBlastComponent::class.java)
        groundBlastComponent.init(scalePace, duration, fadeOutPace)
        entity!!.add(groundBlastComponent)
        return this
    }

    override fun addTurretComponent(base: Entity, followBase: Boolean, cannon: Entity?): EntityBuilder {
        val turretComponent = TurretComponent(base, followBase, cannon)
        entity!!.add(turretComponent)
        return this
    }

    override fun addEnemyComponent(): EntityBuilderImpl {
        val enemyComponent = engine.createComponent(EnemyComponent::class.java)
        entity!!.add(enemyComponent)
        return this
    }

    override fun addStageComponent(): EntityBuilderImpl {
        val enemyComponent = engine.createComponent(StageComponent::class.java)
        entity!!.add(enemyComponent)
        return this
    }

    override fun addAutoAimComponent(): EntityBuilder {
        val autoAimComponent = AutoAimComponent()
        entity!!.add(autoAimComponent)
        return this
    }

    override fun addBaseDoorComponent(initialX: Float, targetX: Float): EntityBuilderImpl {
        val baseDoorComponent = BaseDoorComponent(initialX, targetX)
        entity!!.add(baseDoorComponent)
        return this
    }

    override fun addTurretBaseComponent(): EntityBuilder {
        val turretComponent = TurretBaseComponent()
        entity!!.add(turretComponent)
        return this
    }

    override fun addChildModelInstanceComponent(gameModelInstance: GameModelInstance): EntityBuilder {
        val childModelInstanceComponent = ChildModelInstanceComponent(gameModelInstance)
        entity!!.add(childModelInstanceComponent)
        return this
    }

    override fun addPhysicsComponent(
        shape: btCollisionShape,
        collisionFlag: Int,
        transform: Matrix4,
        applyGravity: Boolean,
    ): EntityBuilderImpl {
        val physicsComponent = addPhysicsComponentToEntity(
            entity!!,
            shape,
            1F,
            collisionFlag,
            transform,
            applyGravity,
        )
        entity!!.add(physicsComponent)
        return this
    }

    override fun finish(): Entity {
        val result = entity
        entity = null
        return result!!
    }

    override fun addPhysicsComponentPooled(
        entity: Entity,
        rigidBodyPool: RigidBodyPool,
        collisionFlag: Int,
        transform: Matrix4,
        applyGravity: Boolean
    ): PhysicsComponent {
        return addPhysicsComponent(
            entity,
            rigidBodyPool.obtain(),
            collisionFlag,
            transform,
            applyGravity
        )
    }

    private fun addPhysicsComponent(
        entity: Entity,
        rigidBody: RigidBody,
        collisionFlag: Int?,
        transform: Matrix4?,
        applyGravity: Boolean,
    ): PhysicsComponent {
        rigidBody.angularVelocity = Vector3.Zero
        rigidBody.clearForces()
        rigidBody.setSleepingThresholds(1f, 1f)
        rigidBody.deactivationTime = 5f
        rigidBody.activate()
        rigidBody.activationState = Collision.DISABLE_DEACTIVATION
        rigidBody.angularFactor = Vector3(1F, 1F, 1F)
        rigidBody.friction = 1.5F
        if (collisionFlag != null) {
            rigidBody.collisionFlags = collisionFlag
        }
        if (transform != null) {
            val motionState = rigidBody.motionState as MotionState
            motionState.transformObject = transform
            motionState.setWorldTransform(transform)
        }
        val physicsComponent = engine.createComponent(PhysicsComponent::class.java)
        physicsComponent.init(rigidBody)
        physicsComponent.rigidBody.userData = entity
        entity.add(physicsComponent)
        messageDispatcher.dispatchMessage(
            SystemEvents.PHYSICS_COMPONENT_ADDED_MANUALLY.ordinal,
            entity
        )
        physicsComponent.rigidBody.gravity = if (applyGravity) auxVector.set(0F, -10F, 0F) else auxVector.setZero()
        return physicsComponent
    }

    override fun addPhysicsComponentToEntity(
        entity: Entity,
        shape: btCollisionShape,
        mass: Float,
        collisionFlag: Int,
        transform: Matrix4,
        applyGravity: Boolean
    ): PhysicsComponent {
        val rigidBody = factories.rigidBodyFactory.create(mass, shape, null, transform)
        return addPhysicsComponent(entity, rigidBody, collisionFlag, null, applyGravity)
    }

    fun init(engine: PooledEngine, factories: Factories, messageDispatcher: MessageDispatcher) {
        this.engine = engine
        this.factories = factories
        this.messageDispatcher = messageDispatcher
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
