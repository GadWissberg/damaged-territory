package com.gadarts.returnfire.factories

import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.ecs.systems.character.factories.OpponentCharacterFactory
import com.gadarts.returnfire.ecs.systems.data.pools.RigidBodyFactory

class Factories(
    val rigidBodyFactory: RigidBodyFactory,
    val ghostFactory: GhostFactory,
    val specialEffectsFactory: SpecialEffectsFactory,
    val gameModelInstanceFactory: GameModelInstanceFactory,
    val autoAimShapeFactory: AutoAimShapeFactory,
    val opponentCharacterFactory: OpponentCharacterFactory,
) : Disposable {
    override fun dispose() {
        opponentCharacterFactory.dispose()
    }
}
