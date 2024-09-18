package com.gadarts.returnfire.systems.data.pools

import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.utils.Pool
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.model.PhysicalDefinition

class RigidBodyPool(
    val definition: ModelDefinition,
    private val assetsManager: GameAssetManager,
    private val rigidBodyFactory: RigidBodyFactory,
) : Pool<btRigidBody>() {
    override fun newObject(): btRigidBody {
        val physicalDefinition: PhysicalDefinition = definition.physicalDefinition!!
        return rigidBodyFactory.create(
            physicalDefinition.mass,
            null,
            physicalDefinition.shapeCreator.create(assetsManager.getCachedBoundingBox(definition)),
            null
        )
    }

}
