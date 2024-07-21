package com.gadarts.returnfire.systems.data

import com.badlogic.ashley.core.Entity

class GameSessionDataEntities {
    lateinit var player: Entity
    lateinit var entitiesAcrossRegions: Array<Array<MutableList<Entity>?>>

}
