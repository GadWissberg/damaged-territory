package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Vector3

class AiComponent : Component {
    fun init(target: Entity?, initialHp: Int) {
        this.target = target
        this.lastHpCheck = initialHp
        this.attackReadyTime = 0
        this.attackReady = true
    }

    var returnToBase: Boolean = false
        private set

    fun returnToBase() {
        returnToBase = true
    }

    var lastHpCheck: Int = 0
    val runAway = Vector3()
    var target: Entity? = null
    var attackReadyTime: Long = 0
    var attackReady: Boolean = true

}
