package com.gadarts.returnfire.model

import com.gadarts.returnfire.assets.definitions.ModelDefinition

enum class SimpleCharacterDefinition(
    private val modelDefinition: ModelDefinition,
) : CharacterDefinition {
    PLAYER(ModelDefinition.APACHE);

    override fun getModelDefinition(): ModelDefinition {
        return modelDefinition
    }

    override fun getName(): String {
        return name
    }

    override fun getCharacterType(): CharacterType {
        return CharacterType.SIMPLE
    }

    override fun isRandomizeRotation(): Boolean {
        return false
    }

    override fun isRandomizeScale(): Boolean {
        return false
    }
}
