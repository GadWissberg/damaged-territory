package com.gadarts.returnfire.ecs.components.ai

import com.badlogic.ashley.core.Component

class TurretEnemyAiComponent : Component {
    var attackReadyTime: Long = 0
    var attackReady: Boolean = true

    init {
        this.attackReadyTime = 0
        this.attackReady = true
    }
}
