package com.gadarts.returnfire.systems.data.pools

import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.managers.GameAssetManager

class RigidBodyPools(private val assetsManager: GameAssetManager, private val rigidBodyFactory: RigidBodyFactory) {

    val pools = mutableMapOf<ModelDefinition, RigidBodyPool>()

    fun obtainRigidBodyPool(definition: ModelDefinition): RigidBodyPool {
        return pools.getOrPut(definition) {
            RigidBodyPool(definition, assetsManager, rigidBodyFactory)
        }
    }

}
