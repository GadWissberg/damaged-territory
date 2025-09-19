package com.gadarts.returnfire.ecs.systems.player.react

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.player.PlayerSystem
import com.gadarts.returnfire.managers.GamePlayManagers


class PlayerSystemOnCharacterDeployed(private val playerSystem: PlayerSystem) :
    HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val player = msg.extraInfo as Entity
        if (ComponentsMapper.player.has(player)) {
            gameSessionData.gamePlayData.player = player
            playerSystem.initInputMethod()
        }
    }


}
