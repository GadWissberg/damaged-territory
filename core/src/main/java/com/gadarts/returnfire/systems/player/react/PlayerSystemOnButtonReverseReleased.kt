package com.gadarts.returnfire.systems.player.react

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.player.handlers.movement.VehicleMovementHandler

class PlayerSystemOnButtonReverseReleased(private val movementHandler: VehicleMovementHandler) :
    HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
        movementHandler.onReverseScreenButtonReleased()
    }

}
