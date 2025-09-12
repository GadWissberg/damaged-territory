package com.gadarts.returnfire.ecs.systems.ai.logic

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.ComponentsMapper.baseAi
import com.gadarts.returnfire.ecs.systems.ai.AiGoals
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.shared.data.CharacterColor

abstract class AiCharacterLogic(
    protected val dispatcher: MessageDispatcher,
    private val gameSessionData: GameSessionData
) : Disposable {
    abstract fun preUpdate(character: Entity, deltaTime: Float)
    abstract fun update(character: Entity, deltaTime: Float)
    fun onboard(character: Entity, epsilon: Float) {
        val characterPosition =
            ComponentsMapper.modelInstance.get(character).gameModelInstance.modelInstance.transform.getTranslation(
                auxVector1
            )
        val hangarStageTransform =
            ComponentsMapper.modelInstance.get(baseAi.get(character).target).gameModelInstance.modelInstance.transform
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
        if (GameDebugSettings.AI_AVOID_GOING_BACK_TO_BASE) return false

        val aiComponent = baseAi.get(character)
        val characterComponent = ComponentsMapper.character.get(character)
        val target = aiComponent.target
        val initialHp = characterComponent.definition.getHP()
        return characterComponent.hp <= initialHp / 4F && target != null && !ComponentsMapper.hangarStage.has(target)
    }

    override fun dispose() {

    }

    fun onCharacterCreated(character: Entity) {
        targetPlayerFlag(character)
    }

    private fun targetPlayerFlag(character: Entity) {
        val rivalFlag = gameSessionData.gamePlayData.flags[CharacterColor.BROWN]
        val baseAiComponent = baseAi.get(character)
        if (rivalFlag != null) {
            baseAiComponent.target = rivalFlag
            if (ComponentsMapper.aiTurret.has(character)) {
                ComponentsMapper.aiTurret.get(character).target = rivalFlag
            }
        }
        baseAiComponent.goal = AiGoals.GET_THE_RIVAL_FLAG
    }

    companion object {
        val auxVector1 = Vector3()
        val auxVector2 = Vector3()
    }
}
