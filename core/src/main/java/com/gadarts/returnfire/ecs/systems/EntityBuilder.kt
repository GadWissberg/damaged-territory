package com.gadarts.returnfire.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.CollisionConstants.DISABLE_DEACTIVATION
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.gadarts.returnfire.ecs.components.*
import com.gadarts.returnfire.ecs.components.ai.AiTurretComponent
import com.gadarts.returnfire.ecs.components.ai.ApacheAiComponent
import com.gadarts.returnfire.ecs.components.ai.BaseAiComponent
import com.gadarts.returnfire.ecs.components.ai.GroundCharacterAiComponent
import com.gadarts.returnfire.ecs.components.amb.AmbAnimationComponent
import com.gadarts.returnfire.ecs.components.arm.ArmComponent
import com.gadarts.returnfire.ecs.components.arm.ArmProperties
import com.gadarts.returnfire.ecs.components.bullet.BulletBehavior
import com.gadarts.returnfire.ecs.components.cd.ChildDecal
import com.gadarts.returnfire.ecs.components.model.GameModelInstance
import com.gadarts.returnfire.ecs.components.onboarding.BoardingAnimation
import com.gadarts.returnfire.ecs.components.physics.GhostPhysicsComponent
import com.gadarts.returnfire.ecs.components.physics.PhysicsComponent
import com.gadarts.returnfire.ecs.systems.data.pools.GameParticleEffectPool
import com.gadarts.returnfire.ecs.systems.data.pools.RigidBodyPool
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition
import com.gadarts.shared.assets.definitions.SoundDefinition
import com.gadarts.shared.assets.definitions.external.TextureDefinition
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.AmbDefinition
import com.gadarts.shared.data.definitions.CharacterDefinition

interface EntityBuilder {
    fun begin(): EntityBuilder
    fun addParticleEffectComponent(
        position: Vector3,
        pool: GameParticleEffectPool,
        rotationAroundY: Float = 0F,
        followRelativePosition: Vector3 = Vector3.Zero,
        ttlInSeconds: Int = 0,
        ttlForComponentOnly: Boolean = false,
        followSpecificEntity: Entity? = null
    ): EntityBuilder

    fun addParticleEffectComponentToEntity(
        entity: Entity,
        pool: GameParticleEffectPool,
        followRelativePosition: Vector3 = Vector3.Zero,
        ttlInSeconds: Int = 0,
        ttlForComponentOnly: Boolean = false
    )

    fun finishAndAddToEngine(): Entity
    fun addModelInstanceComponent(
        model: GameModelInstance,
        position: Vector3,
        boundingBox: BoundingBox?,
        direction: Float = 0F,
        hidden: Boolean = false,
        texture: Texture? = null,
        haloEffect: Boolean = false,
    ): EntityBuilder

    fun addModelInstanceComponentToEntity(
        entity: Entity,
        model: GameModelInstance,
        position: Vector3,
        boundingBox: BoundingBox?,
        direction: Float = 0F,
        hidden: Boolean = false,
        texture: Texture? = null,
        haloEffect: Boolean = false,
    ): EntityBuilder

    fun addGroundBlastComponent(scalePace: Float, duration: Int, fadeOutPace: Float): EntityBuilder
    fun addPhysicsComponent(
        shape: btCollisionShape,
        collisionFlag: Int,
        transform: Matrix4,
        gravityScalar: Float,
        mass: Float = 1F
    ): EntityBuilder

    fun addSparkComponent(relativePositionCalculator: ArmComponent.RelativePositionCalculator): EntityBuilder
    fun addCharacterComponent(characterDefinition: CharacterDefinition, color: CharacterColor): EntityBuilder
    fun addBoardingCharacterComponent(color: CharacterColor, boardingAnimation: BoardingAnimation?): EntityBuilder
    fun addPlayerComponent(): EntityBuilder
    fun addPlayerComponentToEntity(entity: Entity)
    fun addTurretBaseComponent(): EntityBuilder
    fun addAmbSoundComponent(sound: Sound): EntityBuilder
    fun addAmbSoundComponentToEntity(entity: Entity, sound: Sound): EntityBuilder
    fun finish(): Entity
    fun addTurretComponent(
        base: Entity,
        followBasePosition: Boolean,
        followBaseRotation: Boolean,
        relativeY: Float,
        cannon: Entity?
    ): EntityBuilder

    fun addPrimaryArmComponent(
        spark: Entity,
        armProperties: ArmProperties,
        bulletBehavior: BulletBehavior
    ): EntityBuilder

    fun addChildDecalComponent(
        decals: List<ChildDecal>,
        visible: Boolean = true,
    ): EntityBuilder

    fun addChildModelInstanceComponent(
        gameModelInstance: GameModelInstance,
        followParentRotation: Boolean,
        relativePosition: Vector3 = Vector3.Zero
    ): EntityBuilder

    fun addChildModelInstanceComponentToEntity(
        entity: Entity,
        gameModelInstance: GameModelInstance,
        followParentRotation: Boolean,
        relativePosition: Vector3 = Vector3.Zero
    ): ChildModelInstanceComponent

    fun addSecondaryArmComponent(
        spark: Entity,
        armProperties: ArmProperties,
        bulletBehavior: BulletBehavior
    ): EntityBuilder

    fun addAmbComponent(rotation: Float, def: AmbDefinition, scale: Vector3): EntityBuilder
    fun addBaseAiComponent(initialHp: Float, target: Entity? = null): EntityBuilder
    fun addBaseAiComponentToEntity(entity: Entity, initialHp: Float, target: Entity? = null): BaseAiComponent
    fun addPhysicsComponentToEntity(
        entity: Entity,
        shape: btCollisionShape,
        mass: Float,
        collisionFlag: Int,
        transform: Matrix4,
        gravityScalar: Float = 0F,
        friction: Float = 1.5F,
        activationState: Int = DISABLE_DEACTIVATION
    ): PhysicsComponent

    fun addGroundComponent(): EntityBuilder
    fun addBulletComponent(
        bulletBehavior: BulletBehavior,
        explosion: ParticleEffectDefinition?,
        explosive: Boolean,
        friendly: Boolean,
        damage: Float,
        destroyOnSky: Boolean
    ): EntityBuilder

    fun addPhysicsComponentPooledToEntity(
        entity: Entity,
        rigidBodyPool: RigidBodyPool,
        collisionFlag: Int,
        transform: Matrix4,
        gravityScalar: Float = 0F
    ): PhysicsComponent

    fun addBaseDoorComponent(initialX: Float, targetX: Float): EntityBuilder
    fun addStageComponent(base: Entity): EntityBuilder
    fun addAutoAimComponent(): EntityBuilder
    fun addElevatorComponent(color: CharacterColor): EntityBuilder
    fun addCrashSoundEmitterComponent(soundToStop: Sound, soundToStopId: Long): EntityBuilder
    fun addCrashSoundEmitterComponentToEntity(entity: Entity, soundToStop: Sound, soundToStopId: Long): EntityBuilder
    fun addLimitedVelocityComponent(maxValue: Float): EntityBuilder
    fun addRoadComponentToEntity(entity: Entity, textureDefinition: TextureDefinition): EntityBuilder
    fun addModelCacheComponentToEntity(tileEntity: Entity)
    fun addAnimationComponentToEntity(
        entity: Entity,
        loop: Boolean,
        modelInstance: ModelInstance
    ): AmbAnimationComponent

    fun addGhostPhysicsComponentToEntity(
        entity: Entity,
        shape: btCollisionShape,
        position: Vector3,
    ): GhostPhysicsComponent

    fun addFlyingPartComponent(): EntityBuilder
    fun addTreeComponentToEntity(entity: Entity): TreeComponent
    fun addDeathSequenceComponent(): EntityBuilder
    fun addDeathSequenceComponentToEntity(
        entity: Entity,
        createExplosionsAround: Boolean = false,
        minExplosions: Int,
        maxExplosions: Int
    ): DeathSequenceComponent

    fun addAmbCorpsePart(destroyOnGroundImpact: Boolean, collisionSound: SoundDefinition?): EntityBuilder
    fun addFenceComponentToEntity(entity: Entity): FenceComponent
    fun addAiTurretComponentToEntity(turret: Entity): AiTurretComponent
    fun addDrowningEffectComponent(): EntityBuilder
    fun addGroundCharacterAiComponentToEntity(entity: Entity): GroundCharacterAiComponent
    fun addApacheAiComponentToEntity(entity: Entity, initialHp: Float): ApacheAiComponent
    fun addTurretEnemyAiComponent(): EntityBuilder
    fun addFrontWheelsComponent(wheel: ModelDefinition): EntityBuilder
    fun addTurretCannonComponent(relativeX: Float, relativeY: Float): EntityBuilder
    fun addBrownComponent(): EntityBuilder
    fun addGreenComponent(): EntityBuilder
    fun addTurretAutomationComponent(): EntityBuilder
    fun addFlagComponentToEntity(entity: Entity, color: CharacterColor): FlagComponent
    fun addFlagFloorComponentToEntity(entity: Entity, color: CharacterColor): FlagFloorComponent
}
