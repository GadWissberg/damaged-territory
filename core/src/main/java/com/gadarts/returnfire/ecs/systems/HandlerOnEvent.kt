package com.gadarts.returnfire.ecs.systems

import com.badlogic.gdx.ai.msg.Telegram
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.ecs.systems.data.GameSessionData

interface HandlerOnEvent {
    fun react(
        msg: Telegram,
        gameSessionData: GameSessionData,
        gamePlayManagers: GamePlayManagers,
    )

}
