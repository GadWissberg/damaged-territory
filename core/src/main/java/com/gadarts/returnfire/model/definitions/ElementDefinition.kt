package com.gadarts.returnfire.model.definitions

import com.gadarts.returnfire.assets.definitions.model.ModelDefinition
import com.gadarts.returnfire.model.ElementType

interface ElementDefinition {
    fun getModelDefinition(): ModelDefinition
    fun getScale(): Float
    fun isRandomizeRotation(): Boolean
    fun getType(): ElementType
    fun getName(): String
    fun isMarksNodeAsBlocked(): Boolean
}
