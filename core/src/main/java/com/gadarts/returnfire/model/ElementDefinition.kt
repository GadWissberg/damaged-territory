package com.gadarts.returnfire.model

import com.gadarts.returnfire.assets.definitions.ModelDefinition

interface ElementDefinition {
    fun getModelDefinition(): ModelDefinition
    fun isRandomizeScale(): Boolean
    fun isRandomizeRotation(): Boolean
    fun getType(): ElementType
    fun getName(): String
}
