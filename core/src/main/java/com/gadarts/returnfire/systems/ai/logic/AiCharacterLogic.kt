package com.gadarts.returnfire.systems.ai.logic

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.systems.events.SystemEvents

abstract class AiCharacterLogic(protected val dispatcher: MessageDispatcher) {
    abstract fun preUpdate(character: Entity, deltaTime: Float)
    abstract fun update(character: Entity, deltaTime: Float)
    fun onboard(characterTransform: Matrix4, hangarStagePosition: Vector3) {
        val characterPosition = characterTransform.getTranslation(auxVector1)
        if (characterPosition.epsilonEquals(
                hangarStagePosition.x,
                characterPosition.y,
                hangarStagePosition.z,
                0.8F
            )
        ) {
            dispatcher.dispatchMessage(
                SystemEvents.CHARACTER_REQUEST_BOARDING.ordinal,
                characterTransform
            )
        }
    }

    companion object {
        val auxVector1 = Vector3()
    }
}
