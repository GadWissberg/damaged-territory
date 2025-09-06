package com.gadarts.returnfire.ecs.systems.character.factories

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.gadarts.shared.data.CharacterColor
import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.ecs.systems.EntityBuilder
import com.gadarts.returnfire.ecs.systems.data.GameSessionData
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.data.definitions.CharacterDefinition
import com.gadarts.shared.data.definitions.SimpleCharacterDefinition
import com.gadarts.shared.data.definitions.TurretCharacterDefinition

class OpponentCharacterFactory(
    assetsManager: GameAssetManager,
    gameSessionData: GameSessionData,
    gameModelInstanceFactory: GameModelInstanceFactory,
    entityBuilder: EntityBuilder,
) : Disposable {
    private val characterFactories = mapOf(
        SimpleCharacterDefinition.APACHE to ApacheFactory(
            assetsManager,
            gameSessionData,
            entityBuilder,
            gameModelInstanceFactory,
        ),
        TurretCharacterDefinition.TANK to TankFactory(
            assetsManager,
            gameSessionData,
            entityBuilder,
            gameModelInstanceFactory
        ),
        TurretCharacterDefinition.JEEP to JeepFactory(
            assetsManager,
            gameSessionData,
            entityBuilder,
            gameModelInstanceFactory
        ),
    )

    fun create(position: Vector3, selected: CharacterDefinition, characterColor: CharacterColor): Entity {
        var opponent: Entity? = null
        characterFactories[selected]?.let { factory ->
            opponent = factory.create(position, characterColor)
        }
        return opponent!!
    }

    override fun dispose() {
        characterFactories.values.forEach { it.dispose() }
    }


}
