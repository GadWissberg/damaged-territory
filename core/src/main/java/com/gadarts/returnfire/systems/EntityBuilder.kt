package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.gadarts.returnfire.assets.ModelDefinition
import com.gadarts.returnfire.components.AmbComponent
import com.gadarts.returnfire.components.AmbSoundComponent
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.BulletComponent
import com.gadarts.returnfire.components.CharacterComponent
import com.gadarts.returnfire.components.GameModelInstance
import com.gadarts.returnfire.components.GroundComponent
import com.gadarts.returnfire.components.ModelInstanceComponent
import com.gadarts.returnfire.components.PlayerComponent
import com.gadarts.returnfire.components.SphereCollisionComponent
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.arm.PrimaryArmComponent
import com.gadarts.returnfire.components.arm.SecondaryArmComponent
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.systems.player.BulletsPool
import kotlin.math.max

class EntityBuilder private constructor() {
    fun addModelInstanceComponent(
        model: Model,
        position: Vector3,
        modelDefinition: ModelDefinition,
        boundingBox: BoundingBox
    ): EntityBuilder {
        val modelInstanceComponent = engine.createComponent(ModelInstanceComponent::class.java)
        modelInstanceComponent.init(model, position, modelDefinition, boundingBox)
        entity!!.add(modelInstanceComponent)
        return instance
    }

    fun addModelInstanceComponent(
        model: GameModelInstance,
        position: Vector3,
    ): EntityBuilder {
        val modelInstanceComponent = engine.createComponent(ModelInstanceComponent::class.java)
        modelInstanceComponent.init(model, position)
        entity!!.add(modelInstanceComponent)
        return instance
    }

    fun finishAndAddToEngine(): Entity {
        engine.addEntity(entity)
        val result = entity
        entity = null
        return result!!
    }

    fun addChildDecalComponent(
        decals: List<ChildDecal>,
        animateRotation: Boolean
    ): EntityBuilder {
        val component = engine.createComponent(ChildDecalComponent::class.java)
        component.init(decals, animateRotation)
        entity!!.add(component)
        return instance

    }

    fun addAmbSoundComponent(sound: Sound): EntityBuilder {
        val ambSoundComponent = engine.createComponent(AmbSoundComponent::class.java)
        ambSoundComponent.init(sound)
        entity!!.add(ambSoundComponent)
        return instance
    }

    fun addCharacterComponent(hp: Int): EntityBuilder {
        val characterComponent = engine.createComponent(CharacterComponent::class.java)
        characterComponent.init(hp)
        entity!!.add(characterComponent)
        return instance
    }

    fun addPlayerComponent(): EntityBuilder {
        val characterComponent = engine.createComponent(PlayerComponent::class.java)
        characterComponent.init()
        entity!!.add(characterComponent)
        return instance
    }

    fun addPrimaryArmComponent(
        decal: Decal,
        armProperties: ArmProperties,
        priCalculateRelativePosition: ArmComponent.CalculateRelativePosition
    ): EntityBuilder {
        return addArmComponent(
            PrimaryArmComponent::class.java,
            decal,
            armProperties,
            priCalculateRelativePosition
        )
    }

    fun addSecondaryArmComponent(
        decal: Decal,
        armProperties: ArmProperties,
        secCalculateRelativePosition: ArmComponent.CalculateRelativePosition
    ): EntityBuilder {
        return addArmComponent(
            SecondaryArmComponent::class.java,
            decal,
            armProperties,
            secCalculateRelativePosition
        )
    }

    private fun addArmComponent(
        armComponentType: Class<out ArmComponent>,
        decal: Decal,
        armProperties: ArmProperties,
        calculateRelativePosition: ArmComponent.CalculateRelativePosition,
    ): EntityBuilder {
        val armComponent = engine.createComponent(armComponentType)
        armComponent.init(decal, armProperties, calculateRelativePosition)
        entity!!.add(armComponent)
        return instance
    }

    fun addBulletComponent(position: Vector3, speed: Float, pool: BulletsPool): EntityBuilder {
        val bulletComponent = engine.createComponent(BulletComponent::class.java)
        bulletComponent.init(position, speed, pool)
        entity!!.add(bulletComponent)
        return instance
    }

    fun addAmbComponent(scale: Vector3, rotation: Float): EntityBuilder {
        val ambComponent = engine.createComponent(AmbComponent::class.java)
        ambComponent.init(scale, rotation)
        entity!!.add(ambComponent)
        return instance
    }

    fun addGroundComponent(): EntityBuilder {
        val groundComponent = engine.createComponent(GroundComponent::class.java)
        entity!!.add(groundComponent)
        return instance
    }

    fun addSphereCollisionComponent(model: Model): EntityBuilder {
        val collisionComponent = engine.createComponent(SphereCollisionComponent::class.java)
        val box = model.calculateBoundingBox(auxBoundingBox)
        val radius = max(max(box.width / 2F, box.height / 2F), box.depth / 2F)
        collisionComponent.init(radius)
        entity!!.add(collisionComponent)
        return instance
    }

    companion object {

        private lateinit var instance: EntityBuilder
        var entity: Entity? = null
        lateinit var engine: PooledEngine
        private val auxBoundingBox = BoundingBox()
        fun begin(): EntityBuilder {
            entity = engine.createEntity()
            return instance
        }

        fun initialize(engine: PooledEngine) {
            this.engine = engine
            this.instance = EntityBuilder()
        }
    }
}
