package com.gadarts.returnfire.ecs.systems.ai.logic.ground

import com.badlogic.gdx.physics.bullet.collision.btPairCachingGhostObject
import com.gadarts.returnfire.ecs.systems.ai.logic.goals.AiCharacterGoals
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.managers.GamePlayManagers

class AiTankVehicleLogic(
    gameSessionData: GameSessionData,
    autoAim: btPairCachingGhostObject,
    gamePlayManagers: GamePlayManagers
) : AiGroundVehicleLogic(gameSessionData, gamePlayManagers, autoAim) {
    init {
        goal = AiCharacterGoals.ATTACK_TARGET
    }


}
