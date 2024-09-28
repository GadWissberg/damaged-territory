package com.gadarts.returnfire.systems.data.pools

import com.badlogic.gdx.utils.Pool
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.components.physics.RigidBody
import com.gadarts.returnfire.model.PooledObjectPhysicalDefinition

class RigidBodyPool(
    val definition: ModelDefinition,
    private val assetsManager: GameAssetManager,
    private val rigidBodyFactory: RigidBodyFactory,
) : Pool<RigidBody>() {
    override fun newObject(): RigidBody {
        val pooledObjectPhysicalDefinition: PooledObjectPhysicalDefinition =
            definition.pooledObjectPhysicalDefinition!!
        return rigidBodyFactory.create(
            pooledObjectPhysicalDefinition.mass,
            pooledObjectPhysicalDefinition.shapeCreator.create(
                assetsManager.getCachedBoundingBox(
                    definition
                )
            ),
            this,
        )
    }

}
