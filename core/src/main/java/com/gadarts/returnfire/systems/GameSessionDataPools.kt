package com.gadarts.returnfire.systems

import com.badlogic.gdx.math.collision.BoundingBox
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.systems.player.BulletsPool

class GameSessionDataPools(assetsManager: GameAssetManager) {
    val priBulletsPool: BulletsPool = BulletsPool(
        assetsManager.getAssetByDefinition(ModelDefinition.BULLET),
        BoundingBox(assetsManager.getCachedBoundingBox(ModelDefinition.BULLET)),
        ModelDefinition.BULLET
    )
    val secBulletsPool: BulletsPool = BulletsPool(
        assetsManager.getAssetByDefinition(ModelDefinition.MISSILE),
        BoundingBox(assetsManager.getCachedBoundingBox(ModelDefinition.MISSILE)),
        ModelDefinition.MISSILE
    )

}
