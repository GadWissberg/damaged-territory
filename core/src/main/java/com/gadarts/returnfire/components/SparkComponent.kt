package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Entity

class SparkComponent : GameComponent() {

    lateinit var relativePositionCalculator: ArmComponent.RelativePositionCalculator
    lateinit var parent: Entity

    override fun reset() {

    }

    fun init(relativePositionCalculator: ArmComponent.RelativePositionCalculator) {
        this.relativePositionCalculator = relativePositionCalculator
    }

}
