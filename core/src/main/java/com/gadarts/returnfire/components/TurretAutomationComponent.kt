package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Entity

class TurretAutomationComponent : Component {

    var enabled: Boolean = false
    var target: Entity? = null
}
