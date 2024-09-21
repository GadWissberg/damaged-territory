package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity

class SparkComponent(val relativePositionCalculator: ArmComponent.RelativePositionCalculator) : Component {

    lateinit var parent: Entity


}
