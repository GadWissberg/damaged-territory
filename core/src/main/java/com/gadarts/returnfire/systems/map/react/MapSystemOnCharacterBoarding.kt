package com.gadarts.returnfire.systems.map.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.map.MapSystem

class MapSystemOnCharacterBoarding(private val mapSystem: MapSystem) : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val boardingComponent = ComponentsMapper.boarding.get(gameSessionData.gamePlayData.player)
        if (boardingComponent.boardingAnimation == null && boardingComponent.isOnboarding()) {
            val base = mapSystem.findBase(msg.extraInfo as Entity)
            mapSystem.closeDoors(base)
        }
    }

}
