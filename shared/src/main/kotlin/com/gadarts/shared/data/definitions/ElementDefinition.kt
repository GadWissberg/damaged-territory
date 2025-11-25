package com.gadarts.shared.data.definitions

import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.type.ElementType

interface ElementDefinition {
    fun getModelDefinition(): ModelDefinition
    fun getScale(): Float
    fun isRandomizeRotation(): Boolean
    fun getType(): ElementType
    fun getName(): String
    fun isMarksNodeAsBlocked(): Boolean
    fun isPlaceable(): Boolean
    fun customTexturePerColor(): String?
}
