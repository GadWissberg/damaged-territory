package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.components.*
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.arm.PrimaryArmComponent
import com.gadarts.returnfire.components.arm.SecondaryArmComponent
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.model.ModelInstanceComponent
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.model.AmbDefinition
import com.gadarts.returnfire.systems.data.GameParticleEffectPool
import com.gadarts.returnfire.systems.events.SystemEvents

class EntityBuilder private constructor() {

    fun addModelInstanceComponent(
        model: GameModelInstance,
        position: Vector3,
        calculateBoundingBox: Boolean,
        direction: Float = 0F,
        hidden: Boolean = false
    ): EntityBuilder {
        val modelInstanceComponent = engine.createComponent(ModelInstanceComponent::class.java)
        modelInstanceComponent.init(model, position, calculateBoundingBox, direction, hidden)
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

    fun addIndependentDecalComponent(
        textureRegion: TextureRegion,
        lifeInMillis: Long,
        position: Vector3
    ): EntityBuilder {
        val component = engine.createComponent(IndependentDecalComponent::class.java)
        val decal = createDecal(textureRegion)
        decal.position = position
        component.init(decal, lifeInMillis)
        entity!!.add(component)
        return instance
    }

    private fun createDecal(texture: TextureRegion): Decal {
        return Decal.newDecal(
            texture.regionWidth * DECAL_SCALE,
            texture.regionHeight * DECAL_SCALE,
            texture,
            true
        )
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
        spark: Entity,
        armProperties: ArmProperties,
        bulletBehavior: BulletBehavior
    ): EntityBuilder {
        return addArmComponent(
            PrimaryArmComponent::class.java,
            spark,
            armProperties,
            bulletBehavior
        )
    }

    fun addSecondaryArmComponent(
        spark: Entity,
        armProperties: ArmProperties,
        bulletBehavior: BulletBehavior
    ): EntityBuilder {
        return addArmComponent(
            SecondaryArmComponent::class.java,
            spark,
            armProperties,
            bulletBehavior
        )
    }

    private fun addArmComponent(
        armComponentType: Class<out ArmComponent>,
        spark: Entity,
        armProperties: ArmProperties,
        bulletBehavior: BulletBehavior
    ): EntityBuilder {
        ComponentsMapper.spark.get(spark).parent = entity!!
        val armComponent = engine.createComponent(armComponentType)
        armComponent.init(spark, armProperties, bulletBehavior)
        entity!!.add(armComponent)
        return instance
    }

    fun addBulletComponent(
        behavior: BulletBehavior,
        explosion: ParticleEffectDefinition?,
        explosive: Boolean,
        friendly: Boolean
    ): EntityBuilder {
        val bulletComponent = engine.createComponent(BulletComponent::class.java)
        bulletComponent.init(behavior, explosion, explosive, friendly)
        entity!!.add(bulletComponent)
        return instance
    }

    fun addAmbComponent(scale: Vector3, rotation: Float, def: AmbDefinition): EntityBuilder {
        val ambComponent = engine.createComponent(AmbComponent::class.java)
        ambComponent.init(scale, rotation, def)
        entity!!.add(ambComponent)
        return instance
    }

    fun addGroundComponent(): EntityBuilder {
        val groundComponent = engine.createComponent(GroundComponent::class.java)
        entity!!.add(groundComponent)
        return instance
    }


    fun addParticleEffectComponent(
        position: Vector3,
        pool: GameParticleEffectPool,
        rotationAroundY: Float = 0F,
        thisEntityAsParent: Boolean = false
    ): EntityBuilder {
        val effect: ParticleEffect = pool.obtain()
        val particleEffectComponent = engine.createComponent(
            ParticleEffectComponent::class.java
        )
        particleEffectComponent.init(effect, pool.definition, if (thisEntityAsParent) entity else null)
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
        return instance
    }

    fun addSparkComponent(
        relativePositionCalculator: ArmComponent.RelativePositionCalculator,
    ): EntityBuilder {
        val sparkComponent = engine.createComponent(SparkComponent::class.java)
        sparkComponent.init(relativePositionCalculator)
        entity!!.add(sparkComponent)
        return instance
    }

    fun addGroundBlastComponent(scalePace: Float, duration: Int, fadeOutPace: Float): EntityBuilder {
        val groundBlastComponent = engine.createComponent(GroundBlastComponent::class.java)
        groundBlastComponent.init(scalePace, duration, fadeOutPace)
        entity!!.add(groundBlastComponent)
        return instance
    }

    fun addTurretComponent(): EntityBuilder {
        entity!!.add(TurretComponent())
        return instance
    }

    fun addEnemyComponent(): EntityBuilder {
        val enemyComponent = engine.createComponent(EnemyComponent::class.java)
        entity!!.add(enemyComponent)
        return instance
    }

    fun finish(): Entity {
        val result = entity
        entity = null
        return result!!
    }

    companion object {
        private const val DECAL_SCALE = 0.005F
        private lateinit var instance: EntityBuilder
        var entity: Entity? = null
        lateinit var engine: PooledEngine

        fun begin(): EntityBuilder {
            entity = engine.createEntity()
            return instance
        }

        fun initialize(engine: PooledEngine) {
            this.engine = engine
            this.instance = EntityBuilder()
        }

        fun addPhysicsComponent(
            shape: btCollisionShape,
            entity: Entity,
            dispatcher: MessageDispatcher? = null,
            transform: Matrix4 = Matrix4(),
            mass: Float = 0F,
            collisionFlag: Int? = null
        ): PhysicsComponent {
            val physicsComponent = engine.createComponent(PhysicsComponent::class.java)
            physicsComponent.init(shape, mass, transform, collisionFlag)
            physicsComponent.rigidBody.userData = entity
            entity.add(physicsComponent)
            dispatcher?.dispatchMessage(
                SystemEvents.PHYSICS_COMPONENT_ADDED_MANUALLY.ordinal,
                entity
            )
            return physicsComponent
        }
    }
}
