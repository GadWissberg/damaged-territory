package com.gadarts.returnfire.ecs.systems.data.pools

import com.badlogic.gdx.graphics.g3d.particles.ParticleEffect
import com.badlogic.gdx.utils.Pool
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.ParticleEffectDefinition

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
