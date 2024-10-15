package com.gadarts.returnfire.systems

import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.SoundPlayer
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.systems.bullet.BulletSystem.Companion.auxBoundingBox
import com.gadarts.returnfire.systems.data.GameSessionData

class SpecialEffectsGenerator(
    private val gameSessionData: GameSessionData,
    private val soundPlayer: SoundPlayer,
    private val assetsManager: GameAssetManager
) {
    private val waterSplashSounds by lazy {
        assetsManager.getAllAssetsByDefinition(
            SoundDefinition.WATER_SPLASH
        )
    }
    private val waterSplashFloorTexture: Texture by lazy { assetsManager.getTexture("water_splash_floor") }

    fun generateWaterSplash(position: Vector3) {
        EntityBuilder.begin()
            .addParticleEffectComponent(
                position,
                gameSessionData.pools.particleEffectsPools.obtain(
                    ParticleEffectDefinition.WATER_SPLASH
                )
            ).finishAndAddToEngine()
        soundPlayer.play(
            waterSplashSounds.random(),
        )
        addGroundBlast(position, waterSplashFloorTexture, 0.5F, 1.01F, 2000, 0.01F)
    }

    fun addGroundBlast(
        position: Vector3,
        texture: Texture,
        startingScale: Float,
        scalePace: Float,
        duration: Int,
        fadeOutPace: Float
    ) {
        val gameModelInstance = gameSessionData.groundBlastPool.obtain()
        val modelInstance = gameModelInstance.modelInstance
        modelInstance.transform.setToScaling(1F, 1F, 1F)
        val material = modelInstance.materials.get(0)
        val blendingAttribute = material.get(BlendingAttribute.Type) as BlendingAttribute
        blendingAttribute.opacity = 1F
        val textureAttribute =
            material.get(TextureAttribute.Diffuse) as TextureAttribute
        textureAttribute.textureDescription.texture = texture
        EntityBuilder.begin()
            .addModelInstanceComponent(
                gameModelInstance,
                position,
                auxBoundingBox.ext(Vector3.Zero, 1F)
            )
            .addGroundBlastComponent(scalePace, duration, fadeOutPace)
            .finishAndAddToEngine()
        modelInstance.transform.scl(startingScale)
    }

}
