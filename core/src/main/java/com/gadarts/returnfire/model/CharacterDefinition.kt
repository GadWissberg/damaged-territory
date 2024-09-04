package com.gadarts.returnfire.model

interface CharacterDefinition : ElementDefinition {
    override fun getType(): ElementType {
        return ElementType.CHARACTER
    }

    fun getCharacterType(): CharacterType
}
