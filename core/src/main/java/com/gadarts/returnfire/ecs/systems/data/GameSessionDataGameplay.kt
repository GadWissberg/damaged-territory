package com.gadarts.returnfire.ecs.systems.data

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.ecs.systems.data.pools.GameSessionDataPools
import com.gadarts.returnfire.ecs.systems.player.handlers.movement.VehicleMovementHandler
import com.gadarts.shared.GameAssetManager

class GameSessionDataGameplay(assetsManager: GameAssetManager) : Disposable {
    lateinit var player: Entity
    var sessionFinished: Boolean = false
    lateinit var playerMovementHandler: VehicleMovementHandler
    val pools by lazy { GameSessionDataPools(assetsManager) }
    override fun dispose() {
        pools.dispose()
    }

}

