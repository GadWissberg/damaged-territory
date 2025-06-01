package com.gadarts.returnfire.components.arm

import com.badlogic.gdx.math.collision.BoundingBox
import com.gadarts.shared.assets.definitions.model.ModelDefinition

class ArmRenderData(
    val modelDefinition: ModelDefinition,
    val boundingBox: BoundingBox,
    val initialRotationAroundZ: Float = 0F,
)
