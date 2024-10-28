package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.model.TurretCharacterDefinition
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.player.handlers.PlayerShootingHandler

class PlayerFactory(
    assetsManager: GameAssetManager,
    gameSessionData: GameSessionData,
    playerShootingHandler: PlayerShootingHandler
) {
    private val apacheFactory = ApacheFactory(assetsManager, playerShootingHandler, gameSessionData)
    private val tankFactory = TankFactory(assetsManager, gameSessionData)

    fun create(placedPlayer: PlacedElement): Entity {
        var player: Entity? = null
        if (placedPlayer.definition == SimpleCharacterDefinition.APACHE) {
            player = apacheFactory.create(placedPlayer)
        } else if (placedPlayer.definition == TurretCharacterDefinition.TANK) {
            player = tankFactory.create(placedPlayer)
        }
        @Suppress("KotlinConstantConditions")
        if (GameDebugSettings.FORCE_PLAYER_HP >= 0) {
            ComponentsMapper.character.get(player).hp = GameDebugSettings.FORCE_PLAYER_HP
        }
        return player!!
    }


}
