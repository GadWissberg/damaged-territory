package com.gadarts.returnfire.systems.data

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.systems.data.pools.GameSessionDataPools
import com.gadarts.returnfire.systems.player.handlers.movement.VehicleMovementHandler

class GameSessionDataGameplay(assetsManager: GameAssetManager) : Disposable {
    var player: Entity? = null
    var sessionFinished: Boolean = false
    lateinit var playerMovementHandler: VehicleMovementHandler
    val pools by lazy { GameSessionDataPools(assetsManager) }
    override fun dispose() {
        pools.dispose()
    }

}

