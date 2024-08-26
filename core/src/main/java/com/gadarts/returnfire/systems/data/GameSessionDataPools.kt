package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Pool
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.systems.player.GameModelInstancePool

class GameSessionDataPools(assetsManager: GameAssetManager) {
    val gameModelInstancePools = mapOf<ModelDefinition, Pool<GameModelInstance>>(
        ModelDefinition.BULLET to GameModelInstancePool(
            assetsManager.getAssetByDefinition(ModelDefinition.BULLET),
            BoundingBox(assetsManager.getCachedBoundingBox(ModelDefinition.BULLET)),
            ModelDefinition.BULLET
        ),
        ModelDefinition.MISSILE to GameModelInstancePool(
            assetsManager.getAssetByDefinition(ModelDefinition.MISSILE),
            BoundingBox(assetsManager.getCachedBoundingBox(ModelDefinition.MISSILE)),
            ModelDefinition.MISSILE
        )
    )
}
