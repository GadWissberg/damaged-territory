package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.gadarts.returnfire.components.model.GameModelInstance

class ChildModelInstanceComponent(val gameModelInstance: GameModelInstance) : Component {
    var visible: Boolean = true
}
