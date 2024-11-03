package com.gadarts.returnfire

import com.gadarts.returnfire.factories.GameModelInstanceFactory
import com.gadarts.returnfire.systems.SpecialEffectsFactory
import com.gadarts.returnfire.systems.data.pools.RigidBodyFactory

class Factories(
    val rigidBodyFactory: RigidBodyFactory,
    val specialEffectsFactory: SpecialEffectsFactory,
    val gameModelInstanceFactory: GameModelInstanceFactory,
)
