package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.components.AiComponent
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.onboarding.BoardingAnimation
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.model.AmbDefinition
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.systems.data.pools.GameParticleEffectPool
import com.gadarts.returnfire.systems.data.pools.RigidBodyPool

interface EntityBuilder {
    fun begin(): EntityBuilder
    fun addParticleEffectComponent(
        position: Vector3,
        pool: GameParticleEffectPool,
        rotationAroundY: Float = 0F,
        thisEntityAsParent: Boolean = false,
        parentRelativePosition: Vector3 = Vector3.Zero,
        ttlInSeconds: Int = 0
    ): EntityBuilder

    fun finishAndAddToEngine(): Entity
    fun addModelInstanceComponent(
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
        applyGravity: Boolean,
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
    fun addAiComponent(target: Entity? = null): EntityBuilder
    fun addAiComponentToEntity(entity: Entity, target: Entity? = null): AiComponent
    fun addPhysicsComponentToEntity(
        entity: Entity,
        shape: btCollisionShape,
        mass: Float,
        collisionFlag: Int,
        transform: Matrix4,
        applyGravity: Boolean = false
    ): PhysicsComponent

    fun addGroundComponent(): EntityBuilder
    fun addBulletComponent(
        bulletBehavior: BulletBehavior,
        explosion: ParticleEffectDefinition?,
        explosive: Boolean,
        friendly: Boolean,
        damage: Int
    ): EntityBuilder

    fun addPhysicsComponentPooled(
        entity: Entity,
        rigidBodyPool: RigidBodyPool,
        collisionFlag: Int,
        transform: Matrix4,
        applyGravity: Boolean = false
    ): PhysicsComponent

    fun addBaseDoorComponent(initialX: Float, targetX: Float): EntityBuilder
    fun addStageComponent(base: Entity): EntityBuilder
    fun addAutoAimComponent(): EntityBuilder
    fun addBaseComponent(color: CharacterColor): EntityBuilder

}
