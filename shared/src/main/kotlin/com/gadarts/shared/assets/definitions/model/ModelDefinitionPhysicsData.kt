package com.gadarts.shared.assets.definitions.model

import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.assets.definitions.PhysicalShapeCreator
import com.gadarts.shared.data.definitions.PooledObjectPhysicalDefinition

data class ModelDefinitionPhysicsData(
    val pooledObjectPhysicalDefinition: PooledObjectPhysicalDefinition? = null,
    val physicalShapeCreator: PhysicalShapeCreator? = null,
    val centerOfMass: Vector3? = Vector3.Zero
)
