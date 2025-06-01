package com.gadarts.returnfire.systems.data.pools

import com.badlogic.gdx.utils.Pool
import com.gadarts.returnfire.components.physics.RigidBody
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.model.definitions.PooledObjectPhysicalDefinition

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
