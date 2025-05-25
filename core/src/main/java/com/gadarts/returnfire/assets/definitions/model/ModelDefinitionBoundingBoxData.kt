package com.gadarts.returnfire.assets.definitions.model

import com.badlogic.gdx.math.Vector3

data class ModelDefinitionBoundingBoxData(
    val boundingBoxScale: Vector3 = Vector3(1F, 1F, 1F),
    val boundingBoxBias: Vector3 = Vector3.Zero,
)
