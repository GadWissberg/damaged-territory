package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity

class StageComponent(val base: Entity) : Component {

    companion object {
        const val BOTTOM_EDGE_Y: Float = -4F
        const val MAX_Y = -0.9F
    }
}
