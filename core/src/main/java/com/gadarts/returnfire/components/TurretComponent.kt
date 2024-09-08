package com.gadarts.returnfire.components

class TurretComponent : GameComponent() {
    private var currentShootingArm: Int = 1

    override fun reset() {

    }

    fun updateCurrentShootingArm(): Int {
        this.currentShootingArm *= -1
        return this.currentShootingArm
    }


}
