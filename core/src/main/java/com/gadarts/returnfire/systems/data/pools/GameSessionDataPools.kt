package com.gadarts.returnfire.systems.data.pools

import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.systems.player.GameModelInstancePool

class GameSessionDataPools(
    private val assetsManager: GameAssetManager,
    rigidBodyFactory: RigidBodyFactory,
) {
    val gameModelInstancePools = ModelDefinition.entries
        .filter { it.pooledObjectPhysicalDefinition != null }
        .associateWith { createPool(it) }
    val particleEffectsPools = ParticleEffectsPools(assetsManager)
    val rigidBodyPools = RigidBodyPools(assetsManager, rigidBodyFactory)

    private fun createPool(modelDefinition: ModelDefinition) =
        GameModelInstancePool(
            assetsManager.getAssetByDefinition(modelDefinition),
            modelDefinition
        )
}
