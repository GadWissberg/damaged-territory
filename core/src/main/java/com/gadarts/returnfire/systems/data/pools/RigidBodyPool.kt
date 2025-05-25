package com.gadarts.returnfire.systems.data.pools

import com.badlogic.gdx.utils.Pool
import com.gadarts.returnfire.assets.definitions.model.ModelDefinition
import com.gadarts.returnfire.components.physics.RigidBody
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.model.definitions.PooledObjectPhysicalDefinition

class RigidBodyPool(
    val definition: ModelDefinition,
    private val assetsManager: GameAssetManager,
    private val rigidBodyFactory: RigidBodyFactory,
) : Pool<RigidBody>() {
    override fun newObject(): RigidBody {
        val pooledObjectPhysicalDefinition: PooledObjectPhysicalDefinition =
            definition.physicsData.pooledObjectPhysicalDefinition!!
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
