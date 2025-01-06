package com.gadarts.returnfire.systems.data

import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.systems.player.handlers.movement.VehicleMovementHandler

class GameSessionDataGameplay {
    var player: Entity? = null
    var sessionFinished: Boolean = false
    lateinit var playerMovementHandler: VehicleMovementHandler

}
