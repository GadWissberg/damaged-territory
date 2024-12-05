package com.gadarts.returnfire.systems.player

import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.model.TurretCharacterDefinition
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.player.handlers.PlayerShootingHandler

class PlayerFactory(
    assetsManager: GameAssetManager,
    gameSessionData: GameSessionData,
    playerShootingHandler: PlayerShootingHandler,
    gameModelInstanceFactory: GameModelInstanceFactory,
    entityBuilder: EntityBuilder
) {
    private val apacheFactory =
        ApacheFactory(assetsManager, playerShootingHandler, gameSessionData, gameModelInstanceFactory, entityBuilder)
    private val tankFactory = TankFactory(assetsManager, gameSessionData, gameModelInstanceFactory, entityBuilder)

    fun create(base: PlacedElement, selected: CharacterDefinition): Entity {
        var player: Entity? = null
        if (selected == SimpleCharacterDefinition.APACHE) {
            player = apacheFactory.create(base)
        } else if (selected == TurretCharacterDefinition.TANK) {
            player = tankFactory.create(base)
        }
        @Suppress("KotlinConstantConditions")
        if (GameDebugSettings.FORCE_PLAYER_HP >= 0) {
            ComponentsMapper.character.get(player).hp = GameDebugSettings.FORCE_PLAYER_HP
        }
        return player!!
    }


}
