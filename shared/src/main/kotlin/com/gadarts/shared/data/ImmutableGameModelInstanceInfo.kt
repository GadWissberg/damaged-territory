package com.gadarts.shared.data

import com.gadarts.shared.assets.definitions.model.ModelDefinition

class ImmutableGameModelInstanceInfo(
    val modelDefinition: ModelDefinition?,
    private val modelIndex: Int? = null,
) : GameModelInstanceInfo {
    override fun getModelIndex(): Int? {
        return modelIndex
    }

    override fun getDefinition(): ModelDefinition? {
        return modelDefinition
    }


}
