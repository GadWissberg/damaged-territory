package com.gadarts.returnfire.factories

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.managers.EcsManager
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.managers.SoundManager
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.bullet.BulletSystem.Companion.auxBoundingBox
import com.gadarts.returnfire.systems.data.GameSessionData

class SpecialEffectsFactory(
    private val gameSessionData: GameSessionData,
    private val soundManager: SoundManager,
    private val assetsManager: GameAssetManager,
    private val entityBuilder: EntityBuilder,
    private val ecs: EcsManager,
    private val gameModelInstanceFactory: GameModelInstanceFactory
) {
    fun generateWaterSplash(position: Vector3, large: Boolean = false) {
        entityBuilder.begin()
            .addParticleEffectComponent(
                position,
                gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(
                    ParticleEffectDefinition.WATER_SPLASH
                )
            ).finishAndAddToEngine()
        soundManager.play(
            waterSplashSounds.random(),
            position
        )
        generateBlast(
            position,
            waterSplashFloorTexture,
            if (large) 2F else 0.5F,
            1.01F,
            2000,
            0.01F
        )
    }

    fun generateExplosion(
        entity: Entity,
        blastRing: Boolean = false,
        addBiasToPosition: Boolean = true,
    ) {
        val transform = ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector1)
        generateExplosion(position, blastRing, addBiasToPosition)
    }

    fun generateExplosion(
        position: Vector3,
        blastRing: Boolean = false,
        addBiasToPosition: Boolean = true,
        playSound: Boolean = true
    ) {
        if (blastRing) {
            generateBlast(position, blastRingTexture, 0.1F, 8F, 500, 0.03F)
        }
        if (addBiasToPosition) {
            position.add(
                MathUtils.random(
                    MathUtils.randomSign() + MED_EXPLOSION_DEATH_SEQUENCE_BIAS,
                ), MathUtils.random(
                    MED_EXPLOSION_DEATH_SEQUENCE_BIAS,
                ), MathUtils.random(
                    MathUtils.randomSign() + MED_EXPLOSION_DEATH_SEQUENCE_BIAS,
                )
            )
        }
        entityBuilder.begin().addParticleEffectComponent(
            position, explosionMedGameParticleEffectPool
        ).finishAndAddToEngine()
        if (playSound) {
            soundManager.play(
                assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION),
                position
            )
        }
    }

    fun generateFlyingParts(
        character: Entity,
        modelDefinition: ModelDefinition = ModelDefinition.FLYING_PART,
        min: Int = 2,
        max: Int = 4,
        mass: Float = 1F,
        minForce: Float = 4F,
        maxForce: Float = 7F,
        relativeOffset: Vector3 = Vector3.Zero,
        addSmokeParticleEffect: Boolean = true,
    ) {
        val transform = if (ComponentsMapper.turretBase.has(character)) {
            val turretModelInstanceComponent =
                ComponentsMapper.modelInstance.get(ComponentsMapper.turretBase.get(character).turret)
            turretModelInstanceComponent.gameModelInstance.modelInstance.transform
        } else {
            ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
        }
        transform.getTranslation(auxVector2)
        auxVector2.add(relativeOffset)
        generateFlyingParts(
            position = auxVector2,
            modelDefinition = modelDefinition,
            shapeScale = 1F,
            min = min,
            max = max,
            mass = mass,
            minForce = minForce,
            maxForce = maxForce,
            addSmokeParticleEffect = addSmokeParticleEffect
        )
    }

    fun generateSmallFlyingParts(position: Vector3) {
        generateFlyingParts(position, ModelDefinition.FLYING_PART_SMALL, 0.5F)
    }

    fun generateBlast(
        position: Vector3,
        texture: Texture,
        startingScale: Float,
        scalePace: Float,
        duration: Int,
        fadeOutPace: Float
    ) {
        val gameModelInstance = gameSessionData.gamePlayData.pools.groundBlastPool.obtain()
        val modelInstance = gameModelInstance.modelInstance
        modelInstance.transform.setToScaling(1F, 1F, 1F)
        val material = modelInstance.materials.get(0)
        val blendingAttribute = material.get(BlendingAttribute.Type) as BlendingAttribute
        blendingAttribute.opacity = 1F
        val textureAttribute =
            material.get(TextureAttribute.Diffuse) as TextureAttribute
        textureAttribute.textureDescription.texture = texture
        entityBuilder.begin()
            .addModelInstanceComponent(
                gameModelInstance,
                position,
                auxBoundingBox.ext(Vector3.Zero, 1F)
            )
            .addGroundBlastComponent(scalePace, duration, fadeOutPace)
            .finishAndAddToEngine()
        modelInstance.transform.scl(startingScale)
    }

    private val flyingPartBoundingBox by lazy {
        assetsManager.getCachedBoundingBox(
            ModelDefinition.FLYING_PART
        )
    }

    private val explosionMedGameParticleEffectPool by lazy {
        gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(
            ParticleEffectDefinition.EXPLOSION_MED
        )
    }

    private val blastRingTexture: Texture by lazy { this.assetsManager.getTexture("blast_ring") }

    private val waterSplashSounds by lazy {
        assetsManager.getAllAssetsByDefinition(
            SoundDefinition.WATER_SPLASH
        )
    }

    private val waterSplashFloorTexture: Texture by lazy { assetsManager.getTexture("water_splash_floor") }

    private fun generateFlyingParts(
        position: Vector3,
        modelDefinition: ModelDefinition,
        shapeScale: Float,
        min: Int = 2,
        max: Int = 4,
        mass: Float = 1F,
        minForce: Float = 4F,
        maxForce: Float = 7F,
        addSmokeParticleEffect: Boolean = true
    ) {
        val numberOfFlyingParts = MathUtils.random(min, max)
        for (i in 0 until numberOfFlyingParts) {
            addFlyingPart(position, modelDefinition, shapeScale, mass, minForce, maxForce, addSmokeParticleEffect)
        }
    }

    private fun addFlyingPart(
        @Suppress("SameParameterValue") position: Vector3,
        modelDefinition: ModelDefinition,
        shapeScale: Float,
        mass: Float = 1F,
        minForce: Float,
        maxForce: Float,
        addSmokeParticleEffect: Boolean = true
    ) {
        val modelInstance = gameModelInstanceFactory.createGameModelInstance(modelDefinition)
        val flyingPart = createFlyingPartEntity(modelInstance, position, shapeScale, mass, addSmokeParticleEffect)
        ComponentsMapper.physics.get(flyingPart).rigidBody.setDamping(0.1F, 0.2F)
        makeFlyingPartFlyAway(flyingPart, minForce, maxForce)
    }

    private fun createFlyingPartEntity(
        gameModelInstance: GameModelInstance,
        position: Vector3,
        shapeScale: Float,
        mass: Float = 1F,
        addSmokeParticleEffect: Boolean
    ): Entity {
        val randomParticleEffect =
            if (MathUtils.random() >= 0.15) ParticleEffectDefinition.SMOKE_UP_LOOP else ParticleEffectDefinition.FIRE_LOOP_SMALL
        val transform = gameModelInstance.modelInstance.transform
        val physicalShapeCreator = gameModelInstance.definition?.physicalShapeCreator
        val entityBuilder = ecs.entityBuilder
            .begin()
            .addModelInstanceComponent(
                gameModelInstance,
                auxVector1.set(position).add(0F, 0.1F, 0F).add(
                    MathUtils.random(-FLYING_PART_POSITION_MAX_BIAS, FLYING_PART_POSITION_MAX_BIAS),
                    MathUtils.random(-FLYING_PART_POSITION_MAX_BIAS, FLYING_PART_POSITION_MAX_BIAS),
                    MathUtils.random(-FLYING_PART_POSITION_MAX_BIAS, FLYING_PART_POSITION_MAX_BIAS)
                ),
                flyingPartBoundingBox
            )
            .addPhysicsComponent(
                physicalShapeCreator?.create()
                    ?: btBoxShape(
                        flyingPartBoundingBox.getDimensions(
                            auxVector1
                        ).scl(0.4F * shapeScale)
                    ),
                CollisionFlags.CF_CHARACTER_OBJECT,
                transform,
                1F,
                mass
            )
        if (addSmokeParticleEffect) {
            entityBuilder
                .addParticleEffectComponent(
                    transform.getTranslation(auxVector1),
                    gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(randomParticleEffect),
                    ttlInSeconds = MathUtils.random(20, 25)
                )
        }
        return entityBuilder
            .addFlyingPartComponent()
            .finishAndAddToEngine()
    }

    private fun makeFlyingPartFlyAway(flyingPart: Entity, minForce: Float, maxForce: Float) {
        val rigidBody = ComponentsMapper.physics.get(flyingPart).rigidBody
        rigidBody.applyCentralImpulse(
            createRandomDirectionUpwards(minForce, maxForce)
        )
        rigidBody.applyTorque(createRandomDirectionUpwards(minForce, maxForce))
    }

    private fun createRandomDirectionUpwards(minForce: Float, maxForce: Float): Vector3 {
        return auxVector1.set(1F, 0F, 0F).mul(
            auxQuat.idt()
                .setEulerAngles(
                    MathUtils.random(360F),
                    MathUtils.random(360F),
                    MathUtils.random(45F, 135F)
                )
        ).scl(MathUtils.random(minForce, maxForce))
    }

    fun generateExplosionForCharacter(
        character: Entity,
        addBiasToPosition: Boolean = true,
    ) {
        val entity =
            if (ComponentsMapper.turretBase.has(character)) ComponentsMapper.turretBase.get(
                character
            ).turret else character
        generateExplosion(entity, addBiasToPosition)
    }

    companion object {
        const val WATER_SPLASH_Y = 0.05F
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxQuat = Quaternion()
        private const val MED_EXPLOSION_DEATH_SEQUENCE_BIAS = 0.1F
        private const val FLYING_PART_POSITION_MAX_BIAS = 0.4F
    }

}
