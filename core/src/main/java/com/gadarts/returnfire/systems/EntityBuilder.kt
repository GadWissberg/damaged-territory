package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.assets.definitions.external.TextureDefinition
import com.gadarts.returnfire.components.AiComponent
import com.gadarts.returnfire.components.AmbAnimationComponent
import com.gadarts.returnfire.components.DeathSequenceComponent
import com.gadarts.returnfire.components.TreeComponent
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.onboarding.BoardingAnimation
import com.gadarts.returnfire.components.physics.GhostPhysicsComponent
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.model.definitions.AmbDefinition
import com.gadarts.returnfire.model.definitions.CharacterDefinition
import com.gadarts.returnfire.systems.data.pools.GameParticleEffectPool
import com.gadarts.returnfire.systems.data.pools.RigidBodyPool

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
        texture: Texture? = null
    ): EntityBuilder

    fun addModelInstanceComponentToEntity(
        entity: Entity,
        model: GameModelInstance,
        position: Vector3,
        boundingBox: BoundingBox?,
        direction: Float = 0F,
        hidden: Boolean = false,
        texture: Texture? = null
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
    fun addTurretComponent(base: Entity, followBase: Boolean, cannon: Entity?): EntityBuilder
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

    fun addSecondaryArmComponent(
        spark: Entity,
        armProperties: ArmProperties,
        bulletBehavior: BulletBehavior
    ): EntityBuilder

    fun addAmbComponent(rotation: Float, def: AmbDefinition, scale: Vector3): EntityBuilder
    fun addAiComponent(initialHp: Float, target: Entity? = null): EntityBuilder
    fun addAiComponentToEntity(entity: Entity, initialHp: Float, target: Entity? = null): AiComponent
    fun addPhysicsComponentToEntity(
        entity: Entity,
        shape: btCollisionShape,
        mass: Float,
        collisionFlag: Int,
        transform: Matrix4,
        gravityScalar: Float = 0F,
        friction: Float = 1.5F,
    ): PhysicsComponent

    fun addGroundComponent(): EntityBuilder
    fun addBulletComponent(
        bulletBehavior: BulletBehavior,
        explosion: ParticleEffectDefinition?,
        explosive: Boolean,
        friendly: Boolean,
        damage: Float
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
    fun addBaseComponent(color: CharacterColor): EntityBuilder
    fun addCrashSoundEmitterComponent(soundToStop: Sound, soundToStopId: Long): EntityBuilder
    fun addCrashSoundEmitterComponentToEntity(entity: Entity, soundToStop: Sound, soundToStopId: Long): EntityBuilder
    fun addLimitedVelocityComponent(maxValue: Float): EntityBuilder
    fun addRoadComponentToEntity(entity: Entity, textureDefinition: TextureDefinition): EntityBuilder
    fun addModelCacheComponentToEntity(tileEntity: Entity)
    fun addAnimationComponentToEntity(entity: Entity, modelInstance: ModelInstance): AmbAnimationComponent
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
}
