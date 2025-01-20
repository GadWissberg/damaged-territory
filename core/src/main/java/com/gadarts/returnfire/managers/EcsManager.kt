package com.gadarts.returnfire.managers

import com.badlogic.ashley.core.PooledEngine
import com.gadarts.returnfire.systems.EntityBuilder

class EcsManager(
    val engine: PooledEngine,
    val entityBuilder: EntityBuilder,
)
