package com.gadarts.returnfire.ecs.systems.ai.logic

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.ecs.components.BrownComponent
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.ComponentsMapper.baseAi
import com.gadarts.returnfire.ecs.components.GreenComponent
import com.gadarts.returnfire.ecs.systems.ai.AiUtils
import com.gadarts.returnfire.ecs.systems.ai.logic.goals.AiCharacterGoals
import com.gadarts.returnfire.ecs.systems.ai.logic.goals.AiOpponentGoal
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.shared.data.CharacterColor

abstract class AiVehicleLogic(
    protected val gamePlayManagers: GamePlayManagers,
    private val gameSessionData: GameSessionData,
) : Disposable {
    protected var goal: AiCharacterGoals = AiCharacterGoals.GET_THE_RIVAL_FLAG
    private val greens by lazy {
        gamePlayManagers.ecs.engine.getEntitiesFor(Family.all(GreenComponent::class.java).get())
    }
    private val browns by lazy {
        gamePlayManagers.ecs.engine.getEntitiesFor(Family.all(BrownComponent::class.java).get())
    }

    open fun preUpdate(character: Entity, deltaTime: Float) {
        val rivalColor =
            if (ComponentsMapper.character.get(character).color == CharacterColor.BROWN) CharacterColor.GREEN else CharacterColor.BROWN
        val target = baseAi.get(character).target
        val noTargetSet = target == null
        if (noTargetSet || ComponentsMapper.flag.has(target) || ComponentsMapper.character.get(target).dead) {
            val nearestRivalCharacter = AiUtils.findNearestRivalCharacter(
                character, if (rivalColor == CharacterColor.BROWN) browns else greens, 20F
            )
            if (nearestRivalCharacter != null) {
                goal = AiCharacterGoals.ATTACK_TARGET
                setTarget(character, nearestRivalCharacter)
            } else if (noTargetSet) {
                goal = AiCharacterGoals.GET_THE_RIVAL_FLAG
                val rivalFlag =
                    gameSessionData.gamePlayData.flags[rivalColor]
                setTarget(character, rivalFlag)
            }
        }
    }


    private fun setTarget(
        character: Entity,
        target: Entity?,
    ) {
        if (target == null) return

        baseAi.get(character).target = target
        if (ComponentsMapper.turretBase.has(character)) {
            ComponentsMapper.aiTurret.get(ComponentsMapper.turretBase.get(character).turret).target = target
        }
    }

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
            gamePlayManagers.dispatcher.dispatchMessage(
                SystemEvents.CHARACTER_REQUEST_BOARDING.ordinal,
                character
            )
        }
    }

    protected fun shouldReturnToBase(
        character: Entity,
    ): Boolean {
        if (gamePlayManagers.assetsManager.gameSettings.aiAvoidGoingBackToBase) return false
        if (gameSessionData.aiOpponentGoal == AiOpponentGoal.BRING_BACK_CHARACTER) return true

        val aiComponent = baseAi.get(character)
        val characterComponent = ComponentsMapper.character.get(character)
        val target = aiComponent.target
        val initialHp = characterComponent.definition.getHP()
        return characterComponent.hp <= initialHp / 4F && target != null && !ComponentsMapper.elevator.has(target)
    }

    override fun dispose() {

    }


    companion object {
        val auxVector1 = Vector3()
        val auxVector2 = Vector3()
        val auxMatrix1 = com.badlogic.gdx.math.Matrix4()
        val auxMatrix2 = com.badlogic.gdx.math.Matrix4()
    }
}
