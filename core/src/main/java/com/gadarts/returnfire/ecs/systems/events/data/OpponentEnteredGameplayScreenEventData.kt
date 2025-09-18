package com.gadarts.returnfire.ecs.systems.events.data

import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.characters.CharacterDefinition

object OpponentEnteredGameplayScreenEventData {
    lateinit var characterColor: CharacterColor
        private set
    lateinit var selectedCharacter: CharacterDefinition
        private set

    fun set(characterColor: CharacterColor, selectedCharacter: CharacterDefinition) {
        this.characterColor = characterColor
        this.selectedCharacter = selectedCharacter
    }


}
