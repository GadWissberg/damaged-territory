package com.gadarts.returnfire.ecs.systems.character.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.character.CharacterSystem
import com.gadarts.returnfire.ecs.systems.data.session.GameSessionData

class CharacterSystemOnAmbSoundComponentAdded(private val characterSystem: CharacterSystem) : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        characterSystem.playAmbSound(msg.extraInfo as Entity, gamePlayManagers)
    }

}
