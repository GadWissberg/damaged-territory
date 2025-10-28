package com.gadarts.returnfire.ecs.systems.map.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData
import com.gadarts.returnfire.ecs.systems.events.SystemEvents
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.characters.TurretCharacterDefinition

class MapSystemOnCharacterDied : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val character = msg.extraInfo as Entity
        val characterComponent = ComponentsMapper.character.get(character)
        if (characterComponent.definition == TurretCharacterDefinition.JEEP) {
            val rivalColor =
                if (characterComponent.color == CharacterColor.BROWN) CharacterColor.GREEN else CharacterColor.BROWN
            val rivalFlag = gameSessionData.gamePlayData.flags[rivalColor]
            if (rivalFlag != null) {
                val rivalFlagComponent = ComponentsMapper.flag.get(rivalFlag)
                if (rivalFlagComponent.follow == character) {
                    rivalFlagComponent.follow = null
                    gamePlayManagers.dispatcher.dispatchMessage(SystemEvents.FLAG_DROPPED.ordinal, rivalFlag)
                }
            }
        }
    }

}
