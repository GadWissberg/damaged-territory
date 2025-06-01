package com.gadarts.returnfire.systems.data.pools

import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.model.ModelDefinition

class RigidBodyPools(private val assetsManager: GameAssetManager, private val rigidBodyFactory: RigidBodyFactory) {

    val pools = mutableMapOf<ModelDefinition, RigidBodyPool>()

    fun obtainRigidBodyPool(definition: ModelDefinition): RigidBodyPool {
        return pools.getOrPut(definition) {
            RigidBodyPool(definition, assetsManager, rigidBodyFactory)
        }
    }

}
