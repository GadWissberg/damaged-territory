package com.gadarts.returnfire.ecs.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity

class ElevatorComponent(val hangar: Entity) : Component {

    var emptyOnboard: Boolean = false

    companion object {
        const val BOTTOM_EDGE_Y: Float = -4F
        const val MAX_Y = -0.9F
    }
}
