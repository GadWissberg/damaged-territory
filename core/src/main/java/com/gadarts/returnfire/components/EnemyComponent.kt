package com.gadarts.returnfire.components

class EnemyComponent : GameComponent() {
    var dead: Boolean = false
    var attackReadyTime: Long = 0
    var attackReady: Boolean = true

    override fun reset() {

    }

}
