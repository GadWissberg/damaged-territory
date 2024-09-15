package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.utils.Pool
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.systems.player.GameModelInstancePool

class GameSessionDataPools(private val assetsManager: GameAssetManager) {
    val gameModelInstancePools = mapOf<ModelDefinition, Pool<GameModelInstance>>(
        ModelDefinition.BULLET to createPool(ModelDefinition.BULLET),
        ModelDefinition.MISSILE to createPool(ModelDefinition.MISSILE),
        ModelDefinition.CANNON_BULLET to createPool(ModelDefinition.CANNON_BULLET),
    )

    val particleEffectsPools = ParticleEffectsPools(assetsManager)

    private fun createPool(modelDefinition: ModelDefinition) =
        GameModelInstancePool(
            assetsManager.getAssetByDefinition(modelDefinition),
            modelDefinition
        )
}
