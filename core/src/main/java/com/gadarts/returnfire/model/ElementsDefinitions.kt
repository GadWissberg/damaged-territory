package com.gadarts.returnfire.model

import com.gadarts.returnfire.assets.definitions.ModelDefinition

interface ElementsDefinitions {
    fun getModelDefinition(): ModelDefinition
    fun isRandomizeScale(): Boolean
    fun isRandomizeRotation(): Boolean
}
