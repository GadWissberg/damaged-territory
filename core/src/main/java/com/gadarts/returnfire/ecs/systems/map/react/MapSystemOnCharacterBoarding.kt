package com.gadarts.returnfire.ecs.systems.map.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.map.MapSystem

class MapSystemOnCharacterBoarding(private val mapSystem: MapSystem) : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val character = msg.extraInfo as Entity
        val boardingComponent = ComponentsMapper.boarding.get(character)
        if (boardingComponent.boardingAnimation == null && boardingComponent.isOnboarding()) {
            val base = mapSystem.findBase(character)
            mapSystem.closeDoors(base)
        }
    }

}
