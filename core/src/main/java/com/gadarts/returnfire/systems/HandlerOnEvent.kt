package com.gadarts.returnfire.systems

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.systems.data.GameSessionData

interface HandlerOnEvent {
    fun react(
        msg: Telegram,
        gameSessionData: GameSessionData,
        managers: Managers,
    )

}
