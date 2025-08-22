package com.gadarts.returnfire.ecs.systems.events.data

import com.badlogic.gdx.physics.bullet.collision.btCollisionObject

object PhysicsCollisionEventData {
    lateinit var colObj1: btCollisionObject
        private set
    lateinit var colObj0: btCollisionObject
        private set

    fun set(colObj0: btCollisionObject, colObj1: btCollisionObject) {
        this.colObj0 = colObj0
        this.colObj1 = colObj1
    }


}
