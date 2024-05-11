package com.gadarts.returnfire.systems.player.react

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.Services
import com.gadarts.returnfire.systems.GameSessionData
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.player.PlayerShootingHandler

class PlayerSystemOnWeaponButtonSecondaryReleased(private val playerShootingHandler: PlayerShootingHandler) :
    HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, services: Services) {
        playerShootingHandler.onSecondaryWeaponButtonReleased()
    }

}
