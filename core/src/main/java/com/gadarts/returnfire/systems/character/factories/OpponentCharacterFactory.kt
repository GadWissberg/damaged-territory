package com.gadarts.returnfire.systems.character.factories

import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.model.SimpleCharacterDefinition
import com.gadarts.returnfire.model.TurretCharacterDefinition
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.data.GameSessionData

class OpponentCharacterFactory(
    assetsManager: GameAssetManager,
    gameSessionData: GameSessionData,
    gameModelInstanceFactory: GameModelInstanceFactory,
    entityBuilder: EntityBuilder,
) {
    private val apacheFactory =
        ApacheFactory(
            assetsManager,
            gameSessionData,
            gameModelInstanceFactory,
            entityBuilder,
        )
    private val tankFactory =
        TankFactory(assetsManager, gameSessionData, gameModelInstanceFactory, entityBuilder)

    fun create(base: PlacedElement, selected: CharacterDefinition, characterColor: CharacterColor): Entity {
        var opponent: Entity? = null
        if (selected == SimpleCharacterDefinition.APACHE) {
            opponent = apacheFactory.create(base, characterColor)
        } else if (selected == TurretCharacterDefinition.TANK) {
            opponent = tankFactory.create(base, characterColor)
        }
        @Suppress("KotlinConstantConditions")
        if (GameDebugSettings.FORCE_PLAYER_HP >= 0) {
            ComponentsMapper.character.get(opponent).hp = GameDebugSettings.FORCE_PLAYER_HP
        }
        return opponent!!
    }


}
