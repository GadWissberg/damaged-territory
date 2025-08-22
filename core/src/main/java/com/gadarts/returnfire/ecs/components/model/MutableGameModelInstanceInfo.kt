package com.gadarts.returnfire.ecs.components.model

import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.GameModelInstanceInfo

class MutableGameModelInstanceInfo : GameModelInstanceInfo {
    private var index: Int? = null
    private var definition: ModelDefinition? = null

    fun set(definition: ModelDefinition?, index: Int?): GameModelInstanceInfo {
        this.definition = definition
        this.index = index
        return this
    }

    override fun getModelIndex(): Int? {
        return index
    }

    override fun getDefinition(): ModelDefinition? {
        return definition
    }

}
