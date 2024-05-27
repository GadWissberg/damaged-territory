package com.gadarts.returnfire.model

import com.gadarts.returnfire.assets.ModelDefinition

enum class CharactersDefinitions(
    private val modelDefinition: ModelDefinition,
) : ElementsDefinitions {
    PLAYER(ModelDefinition.APACHE);

    override fun getModelDefinition(): ModelDefinition {
        return modelDefinition
    }

    override fun isRandomizeRotation(): Boolean {
        return false
    }

    override fun isRandomizeScale(): Boolean {
        return false
    }
}
