package com.gadarts.returnfire.systems.data.pools

import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.managers.GameAssetManager

class ParticleEffectsPools(private val assetsManager: GameAssetManager) {
    val pools = mutableMapOf<ParticleEffectDefinition, GameParticleEffectPool>()

    fun obtain(definition: ParticleEffectDefinition): GameParticleEffectPool {
        return pools.getOrPut(definition) {
            GameParticleEffectPool(definition, assetsManager)
        }
    }
}
