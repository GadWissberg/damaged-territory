package com.gadarts.returnfire.ecs.components.ai

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.math.Vector3

class ApacheAiComponent(initialHp: Float) : Component {
    var lastHpCheck: Float = initialHp
    val runAway = Vector3()

}
