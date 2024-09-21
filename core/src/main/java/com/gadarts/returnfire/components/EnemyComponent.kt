package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component

class EnemyComponent : Component {
    var attackReadyTime: Long = 0
    var attackReady: Boolean = true

}
