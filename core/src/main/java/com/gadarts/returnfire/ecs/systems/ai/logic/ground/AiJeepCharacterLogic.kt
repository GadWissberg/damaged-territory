package com.gadarts.returnfire.ecs.systems.ai.logic.ground

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.systems.ai.logic.goals.AiCharacterGoals
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.shared.data.CharacterColor

class AiJeepCharacterLogic(
    private val gameSessionData: GameSessionData,
    gamePlayManagers: GamePlayManagers,
    autoAim: btPairCachingGhostObject,
) : AiGroundCharacterLogic(gameSessionData, gamePlayManagers, autoAim), Disposable {
    init {
        goal = AiCharacterGoals.GET_THE_RIVAL_FLAG
    }

    override fun preUpdate(character: Entity, deltaTime: Float) {
        super.preUpdate(character, deltaTime)
        val flagComponent = ComponentsMapper.flag.get(gameSessionData.gamePlayData.flags[CharacterColor.BROWN])
        if (goal != AiCharacterGoals.GO_TO_YOUR_FLAG && flagComponent.follow == character) {
            goal = AiCharacterGoals.GO_TO_YOUR_FLAG
            ComponentsMapper.baseAi.get(character).target = gameSessionData.gamePlayData.flags[CharacterColor.GREEN]
        }
    }

    override fun getMaxDistanceForTargetReached(target: Entity): Float {
        return if (ComponentsMapper.flag.has(target)) 0F else super.getMaxDistanceForTargetReached(target)
    }

}
