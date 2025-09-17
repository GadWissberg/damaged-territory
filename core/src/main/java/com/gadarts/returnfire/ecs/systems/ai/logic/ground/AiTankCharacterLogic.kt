package com.gadarts.returnfire.ecs.systems.ai.logic.ground

import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.gadarts.returnfire.ecs.systems.ai.logic.goals.AiCharacterGoals
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.managers.GamePlayManagers

class AiTankCharacterLogic(
    gameSessionData: GameSessionData,
    autoAim: btPairCachingGhostObject,
    gamePlayManagers: GamePlayManagers
) : AiGroundCharacterLogic(gameSessionData, autoAim, gamePlayManagers) {
    init {
        goal = AiCharacterGoals.ATTACK_PLAYER
    }


}