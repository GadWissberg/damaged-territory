package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity

class AiComponent : Component {
    fun init(target: Entity?) {
        this.target = target
    }

    var target: Entity? = null
    var attackReadyTime: Long = 0
    var attackReady: Boolean = true

}
