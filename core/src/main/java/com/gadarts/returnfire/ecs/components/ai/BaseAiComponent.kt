package com.gadarts.returnfire.ecs.components.ai

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.ecs.systems.ai.logic.status.AiStatus

class BaseAiComponent(target: Entity?) : Component {
    var state: AiStatus = AiStatus.PLANNING
    var target: Entity? = null

    init {
        this.target = target

    }

}
