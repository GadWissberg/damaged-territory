package com.gadarts.returnfire.ecs.systems.ai.logic

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.ComponentsMapper.ai
import com.gadarts.returnfire.ecs.systems.events.SystemEvents

abstract class AiCharacterLogic(protected val dispatcher: MessageDispatcher) : Disposable {
    abstract fun preUpdate(character: Entity, deltaTime: Float)
    abstract fun update(character: Entity, deltaTime: Float)
    fun onboard(character: Entity, epsilon: Float) {
        val characterPosition =
            ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val hangarStageTransform =
            ComponentsMapper.modelInstance.get(ai.get(character).target).gameModelInstance.modelInstance.transform
        val hangarStagePosition =
            hangarStageTransform.getTranslation(
                auxVector2
            )
        if (characterPosition.epsilonEquals(
                hangarStagePosition.x,
                characterPosition.y,
                hangarStagePosition.z,
                epsilon
            )
        ) {
            dispatcher.dispatchMessage(
                SystemEvents.CHARACTER_REQUEST_BOARDING.ordinal,
                character
            )
        }
    }

    protected fun shouldReturnToBase(
        character: Entity,
    ): Boolean {
        val aiComponent = ai.get(character)
        val characterComponent = ComponentsMapper.character.get(character)
        val target = aiComponent.target
        val initialHp = characterComponent.definition.getHP()
        return characterComponent.hp <= initialHp / 4F && target != null && !ComponentsMapper.hangarStage.has(target)
    }

    override fun dispose() {

    }
    companion object {
        val auxVector1 = Vector3()
        val auxVector2 = Vector3()
    }
}
