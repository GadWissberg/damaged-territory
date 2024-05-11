package com.gadarts.returnfire.systems

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.Services

interface HandlerOnEvent {
    fun react(
        msg: Telegram,
        gameSessionData: GameSessionData,
        services: Services,
    )

}
