package com.gadarts.returnfire.systems.player.react

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.player.PlayerSystem
import com.gadarts.returnfire.utils.CharacterPhysicsInitializer


class PlayerSystemOnCharacterOffBoarded(private val playerSystem: PlayerSystem) :
    HandlerOnEvent {
    private val characterPhysicsInitializer = CharacterPhysicsInitializer()
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        characterPhysicsInitializer.initialize(gamePlayManagers.entityBuilder, gameSessionData.gamePlayData.player!!)
        playerSystem.initInputMethod()
    }


}
