package com.gadarts.returnfire.ecs.systems.hud.react

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.ecs.systems.HandlerOnEvent
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.returnfire.ecs.systems.hud.HudButtons

class HudSystemOnLandingIndicatorVisibilityChanged(private val hudButtons: HudButtons) : HandlerOnEvent {
    override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
        val onboardButton = hudButtons.onboardButton
        if (onboardButton != null) {
            onboardButton.isVisible = msg.extraInfo as Boolean
        }
    }

}
