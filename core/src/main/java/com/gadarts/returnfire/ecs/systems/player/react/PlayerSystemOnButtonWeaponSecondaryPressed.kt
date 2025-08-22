package com.gadarts.returnfire.ecs.systems.player.react

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.player.handlers.PlayerShootingHandler

class PlayerSystemOnButtonWeaponSecondaryPressed(
    private val playerShootingHandler: PlayerShootingHandler,
) :
    HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        playerShootingHandler.startSecondaryShooting(gameSessionData.gamePlayData.player)
    }

}
