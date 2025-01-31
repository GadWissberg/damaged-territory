package com.gadarts.returnfire.factories

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.managers.SoundPlayer
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.bullet.BulletSystem.Companion.auxBoundingBox
import com.gadarts.returnfire.systems.data.GameSessionData

class SpecialEffectsFactory(
    private val gameSessionData: GameSessionData,
    private val soundPlayer: SoundPlayer,
    private val assetsManager: GameAssetManager,
    private val entityBuilder: EntityBuilder
) {
    private val explosionMedGameParticleEffectPool by lazy {
        gameSessionData.pools.particleEffectsPools.obtain(
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
                gameSessionData.pools.particleEffectsPools.obtain(
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
        val gameModelInstance = gameSessionData.pools.groundBlastPool.obtain()
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
        val position = transform.getTranslation(auxVector)
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

    companion object {
        const val WATER_SPLASH_Y = 0.05F
        val auxVector = Vector3()
        private const val MED_EXPLOSION_DEATH_SEQUENCE_BIAS = 0.1F
    }

}
