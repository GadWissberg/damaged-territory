package com.gadarts.shared.data.definitions

import com.badlogic.gdx.math.Matrix4
import com.gadarts.shared.assets.definitions.model.ModelDefinition

data class RelatedModelToBeRenderedInEditor(
    val relativeTransform: Matrix4,
    val modelDefinition: ModelDefinition,
    val customTexture: String? = null
)
