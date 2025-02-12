package com.gadarts.returnfire.factories

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
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
import com.gadarts.returnfire.managers.SoundPlayer
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.bullet.BulletSystem.Companion.auxBoundingBox
import com.gadarts.returnfire.systems.data.GameSessionData

class SpecialEffectsFactory(
    private val gameSessionData: GameSessionData,
    private val soundPlayer: SoundPlayer,
    private val assetsManager: GameAssetManager,
    private val entityBuilder: EntityBuilder,
    private val ecs: EcsManager
) {
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

    fun generateWaterSplash(position: Vector3, large: Boolean = false) {
        entityBuilder.begin()
            .addParticleEffectComponent(
                position,
                gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(
                    ParticleEffectDefinition.WATER_SPLASH
                )
            ).finishAndAddToEngine()
        soundPlayer.play(
            waterSplashSounds.random(),
            position
        )
        addGroundBlast(
            position,
            waterSplashFloorTexture,
            if (large) 2F else 0.5F,
            1.01F,
            2000,
            0.01F
        )
    }

    fun addGroundBlast(
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

    fun generateExplosion(entity: Entity, blastRing: Boolean = false) {
        val transform = ComponentsMapper.modelInstance.get(entity).gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector1)
        if (blastRing) {
            addGroundBlast(position, blastRingTexture, 0.1F, 8F, 500, 0.03F)
        }
        entityBuilder.begin().addParticleEffectComponent(
            position.add(
                MathUtils.random(
                    MathUtils.randomSign() + MED_EXPLOSION_DEATH_SEQUENCE_BIAS,
                ), MathUtils.random(
                    MED_EXPLOSION_DEATH_SEQUENCE_BIAS,
                ), MathUtils.random(
                    MathUtils.randomSign() + MED_EXPLOSION_DEATH_SEQUENCE_BIAS,
                )
            ), explosionMedGameParticleEffectPool
        ).finishAndAddToEngine()
        soundPlayer.play(
            assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION),
            position
        )
    }

    fun addFlyingParts(character: Entity) {
        val transform = if (ComponentsMapper.turretBase.has(character)) {
            val turretModelInstanceComponent =
                ComponentsMapper.modelInstance.get(ComponentsMapper.turretBase.get(character).turret)
            turretModelInstanceComponent.gameModelInstance.modelInstance.transform
        } else {
            ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform
        }
        transform.getTranslation(auxVector2)
        addFlyingParts(auxVector2)
    }

    fun addFlyingParts(position: Vector3) {
        val model = assetsManager.getAssetByDefinition(ModelDefinition.FLYING_PART)
        addFlyingParts(position, model)
    }

    fun addSmallFlyingParts(position: Vector3) {
        val model = assetsManager.getAssetByDefinition(ModelDefinition.FLYING_PART_SMALL)
        addFlyingParts(position, model)
    }

    private fun addFlyingParts(
        position: Vector3,
        model: Model
    ) {
        val numberOfFlyingParts = MathUtils.random(2, 4)
        for (i in 0 until numberOfFlyingParts) {
            addFlyingPart(position, model)
        }
    }

    private fun addFlyingPart(
        @Suppress("SameParameterValue") position: Vector3,
        model: Model,
    ) {
        val modelInstance = ModelInstance(model)
        val flyingPart = createFlyingPartEntity(modelInstance, position)
        ComponentsMapper.physics.get(flyingPart).rigidBody.setDamping(0.1F, 0.2F)
        makeFlyingPartFlyAway(flyingPart)
    }

    private fun createFlyingPartEntity(
        modelInstance: ModelInstance,
        position: Vector3
    ) = ecs.entityBuilder
        .begin()
        .addModelInstanceComponent(
            GameModelInstance(modelInstance, ModelDefinition.FLYING_PART),
            auxVector1.set(position).add(
                MathUtils.random(-FLYING_PART_POSITION_MAX_BIAS, FLYING_PART_POSITION_MAX_BIAS),
                MathUtils.random(-FLYING_PART_POSITION_MAX_BIAS, FLYING_PART_POSITION_MAX_BIAS),
                MathUtils.random(-FLYING_PART_POSITION_MAX_BIAS, FLYING_PART_POSITION_MAX_BIAS)
            ),
            flyingPartBoundingBox
        )
        .addPhysicsComponent(
            btBoxShape(
                flyingPartBoundingBox.getDimensions(
                    auxVector1
                ).scl(0.4F)
            ),
            CollisionFlags.CF_CHARACTER_OBJECT,
            modelInstance.transform,
            1F,
        )
        .addParticleEffectComponent(
            modelInstance.transform.getTranslation(auxVector1),
            gameSessionData.gamePlayData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_UP_LOOP),
            ttlInSeconds = MathUtils.random(20, 25)
        )
        .finishAndAddToEngine()

    private fun makeFlyingPartFlyAway(flyingPart: Entity) {
        val rigidBody = ComponentsMapper.physics.get(flyingPart).rigidBody
        rigidBody.applyCentralImpulse(
            createRandomDirectionUpwards()
        )
        rigidBody.applyTorque(createRandomDirectionUpwards())
    }

    private fun createRandomDirectionUpwards(): Vector3 {
        return auxVector1.set(1F, 0F, 0F).mul(
            auxQuat.idt()
                .setEulerAngles(
                    MathUtils.random(360F),
                    MathUtils.random(360F),
                    MathUtils.random(45F, 135F)
                )
        ).scl(MathUtils.random(4F, 7F))
    }

    companion object {
        const val WATER_SPLASH_Y = 0.05F
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxQuat = Quaternion()
        private const val MED_EXPLOSION_DEATH_SEQUENCE_BIAS = 0.1F
        private const val FLYING_PART_POSITION_MAX_BIAS = 0.2F
    }

}
