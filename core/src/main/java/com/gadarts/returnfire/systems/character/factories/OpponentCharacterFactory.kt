package com.gadarts.returnfire.systems.character.factories

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.model.PlacedElement
import com.gadarts.returnfire.model.definitions.CharacterDefinition
import com.gadarts.returnfire.model.definitions.SimpleCharacterDefinition
import com.gadarts.returnfire.model.definitions.TurretCharacterDefinition
import com.gadarts.returnfire.systems.EntityBuilder
import com.gadarts.returnfire.systems.data.GameSessionData

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

    fun create(base: PlacedElement, selected: CharacterDefinition, characterColor: CharacterColor): Entity {
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
