package com.gadarts.returnfire.factories

import com.gadarts.returnfire.systems.data.pools.RigidBodyFactory

class Factories(
    val rigidBodyFactory: RigidBodyFactory,
    val specialEffectsFactory: SpecialEffectsFactory,
    val gameModelInstanceFactory: GameModelInstanceFactory,
    val autoAimShapeFactory: AutoAimShapeFactory,
)
