package com.gadarts.returnfire.model

import com.gadarts.returnfire.assets.ModelsDefinitions

interface ElementsDefinitions {
    fun getModelDefinition(): ModelsDefinitions
    fun isRandomizeScale(): Boolean
    fun isRandomizeRotation(): Boolean
}
