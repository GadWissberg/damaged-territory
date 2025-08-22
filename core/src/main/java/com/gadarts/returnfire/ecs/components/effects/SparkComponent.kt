package com.gadarts.returnfire.ecs.components.effects

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity
import com.gadarts.returnfire.components.arm.ArmComponent

class SparkComponent(val relativePositionCalculator: ArmComponent.RelativePositionCalculator) : Component {

    lateinit var parent: Entity


}
