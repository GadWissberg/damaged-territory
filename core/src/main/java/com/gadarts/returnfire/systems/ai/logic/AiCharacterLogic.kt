package com.gadarts.returnfire.systems.ai.logic

import com.badlogic.ashley.core.Entity

interface AiCharacterLogic {
    fun preUpdate(character: Entity, deltaTime: Float)
    fun update(character: Entity, deltaTime: Float)

}
