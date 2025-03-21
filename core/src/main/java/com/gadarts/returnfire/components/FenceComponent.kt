package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity

class FenceComponent : Component {
    var right: Entity? = null
        private set
    var left: Entity? = null
        private set

    fun setNeighbors(left: Entity?, right: Entity?) {
        this.left = left
        this.right = right
    }

}
