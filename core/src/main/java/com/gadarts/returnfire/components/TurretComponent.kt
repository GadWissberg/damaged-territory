package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Entity

class TurretComponent : GameComponent() {
    lateinit var base: Entity
        private set
    private var currentShootingArm: Int = 1
    override fun reset() {

    }

    fun init(base: Entity) {
        this.base = base
    }

    fun updateCurrentShootingArm(): Int {
        this.currentShootingArm *= -1
        return this.currentShootingArm
    }


}
