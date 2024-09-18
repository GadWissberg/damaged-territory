package com.gadarts.returnfire.systems.data.pools

import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition

class RigidBodyPools(private val assetsManager: GameAssetManager, private val rigidBodyFactory: RigidBodyFactory) {

    val pools = mutableMapOf<ModelDefinition, RigidBodyPool>()

    fun obtainRigidBodyPool(definition: ModelDefinition): RigidBodyPool {
        return pools.getOrPut(definition) {
            RigidBodyPool(definition, assetsManager, rigidBodyFactory)
        }
    }

}
