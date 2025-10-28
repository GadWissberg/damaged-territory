package com.gadarts.returnfire.ecs.systems.data.session

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.FlagComponent
import com.gadarts.returnfire.ecs.systems.data.OpponentData
import com.gadarts.returnfire.ecs.systems.data.pools.GameSessionDataPools
import com.gadarts.returnfire.ecs.systems.player.handlers.movement.VehicleMovementHandler
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.data.CharacterColor

class GameSessionDataGameplay(
    val opponentsData: Map<CharacterColor, OpponentData>,
    assetsManager: GameAssetManager,
    engine: PooledEngine,
) : Disposable {
    var gameSessionState: GameSessionState = GameSessionState.PLAYING
    val flags: Map<CharacterColor, Entity> by lazy {
        engine.getEntitiesFor(Family.all(FlagComponent::class.java).get())
            .associateBy({ ComponentsMapper.flag.get(it).color }, { it })
    }
    var player: Entity? = null
    lateinit var playerMovementHandler: VehicleMovementHandler
    val pools by lazy { GameSessionDataPools(assetsManager) }
    override fun dispose() {
        pools.dispose()
    }

}
