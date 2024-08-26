package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.utils.Pool
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition

class GameParticleEffectPool(
    val definition: ParticleEffectDefinition,
    private val assetsManager: GameAssetManager,
) : Pool<ParticleEffect>() {
    override fun newObject(): ParticleEffect {
        val effect = assetsManager.getAssetByDefinition(definition).copy()
        effect.init()
        return effect
    }

}
