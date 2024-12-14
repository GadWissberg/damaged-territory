package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component

class BaseDoorComponent(val initialX: Float, val targetX: Float) : Component {
    private var doorMoveState: Int = 1

}
