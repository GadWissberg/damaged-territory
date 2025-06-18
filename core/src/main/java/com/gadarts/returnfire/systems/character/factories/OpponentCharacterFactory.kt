package com.gadarts.returnfire.systems.character.factories

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.map.GameMapPlacedObject
import com.gadarts.shared.model.definitions.CharacterDefinition
import com.gadarts.shared.model.definitions.SimpleCharacterDefinition
import com.gadarts.shared.model.definitions.TurretCharacterDefinition

class OpponentCharacterFactory(
    assetsManager: GameAssetManager,
    gameSessionData: GameSessionData,
    gameModelInstanceFactory: GameModelInstanceFactory,
    entityBuilder: EntityBuilder,
) : Disposable {
    private val apacheFactory =
        ApacheFactory(
            assetsManager,
            gameSessionData,
            entityBuilder,
            gameModelInstanceFactory,
        )
    private val tankFactory =
        TankFactory(assetsManager, gameSessionData, entityBuilder, gameModelInstanceFactory)

    fun create(base: GameMapPlacedObject, selected: CharacterDefinition, characterColor: CharacterColor): Entity {
        var opponent: Entity? = null
        if (selected == SimpleCharacterDefinition.APACHE) {
            opponent = apacheFactory.create(base, characterColor)
        } else if (selected == TurretCharacterDefinition.TANK) {
            opponent = tankFactory.create(base, characterColor)
        }
        return opponent!!
    }

    override fun dispose() {
        apacheFactory.dispose()
        tankFactory.dispose()
    }


}
