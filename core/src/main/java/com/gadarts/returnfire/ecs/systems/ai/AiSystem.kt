package com.gadarts.returnfire.ecs.systems.ai

import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.ecs.systems.ai.logic.AiLogicHandler
import com.gadarts.shared.data.definitions.characters.CharacterDefinition

interface AiSystem {
    fun invokeAiComponentInitializer(definition: CharacterDefinition, character: Entity)
    fun getAiLogicHandler(): AiLogicHandler

}
