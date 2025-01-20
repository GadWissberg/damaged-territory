package com.gadarts.returnfire.model.definitions

import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.model.ElementType

interface ElementDefinition {
    fun getModelDefinition(): ModelDefinition
    fun isRandomizeScale(): Boolean
    fun isRandomizeRotation(): Boolean
    fun getType(): ElementType
    fun getName(): String
}