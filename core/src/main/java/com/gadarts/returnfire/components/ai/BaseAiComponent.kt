package com.gadarts.returnfire.components.ai

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.systems.ai.AiStatus

class BaseAiComponent(target: Entity?) : Component {
    var state: AiStatus = AiStatus.PLANNING
    var target: Entity? = null

    init {
        this.target = target

    }

}
