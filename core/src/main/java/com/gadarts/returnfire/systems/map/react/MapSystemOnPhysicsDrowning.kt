package com.gadarts.returnfire.systems.map.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.factories.SpecialEffectsFactory
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.data.GameSessionDataMap.Companion.DROWNING_HEIGHT
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.utils.GeneralUtils

class MapSystemOnPhysicsDrowning : HandlerOnEvent {

    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val entity = msg.extraInfo as Entity
        val drowningHeight = -1 *
                if (ComponentsMapper.amb.has(entity)) ComponentsMapper.amb.get(entity).def.drowningHeight else DROWNING_HEIGHT
        val position = GeneralUtils.getPositionOfModel(entity)
        if (position.y > drowningHeight) return

        position.set(
            position.x,
            SpecialEffectsFactory.WATER_SPLASH_Y,
            position.z
        )
        gamePlayManagers.factories.specialEffectsFactory.generateWaterSplash(
            position, ComponentsMapper.character.has(entity)
        )
        gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.REMOVE_ENTITY.ordinal, entity)
    }

}
