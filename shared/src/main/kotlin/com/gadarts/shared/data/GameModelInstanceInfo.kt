package com.gadarts.shared.data

import com.gadarts.shared.assets.definitions.model.ModelDefinition

interface GameModelInstanceInfo {
    fun getModelIndex(): Int?
    fun getDefinition(): ModelDefinition?

}
