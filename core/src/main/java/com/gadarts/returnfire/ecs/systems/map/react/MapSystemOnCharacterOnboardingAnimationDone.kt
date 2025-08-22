package com.gadarts.returnfire.ecs.systems.map.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.map.MapSystem

class MapSystemOnCharacterOnboardingAnimationDone(private val mapSystem: MapSystem) : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val base = mapSystem.findBase(msg.extraInfo as Entity)
        mapSystem.closeDoors(base)
        mapSystem.hideLandingMark()
    }

}
