package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity

class TurretComponent(val base: Entity) : Component {
    private var currentShootingArm: Int = 1


    fun updateCurrentShootingArm(): Int {
        this.currentShootingArm *= -1
        return this.currentShootingArm
    }


}
