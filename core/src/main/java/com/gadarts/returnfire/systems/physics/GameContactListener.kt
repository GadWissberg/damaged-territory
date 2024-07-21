package com.gadarts.returnfire.systems.physics

import com.badlogic.gdx.physics.bullet.collision.ContactListener
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject

class GameContactListener : ContactListener() {

    override fun onContactAdded(
        colObj0: btCollisionObject?,
        partId0: Int,
        index0: Int,
        colObj1: btCollisionObject?,
        partId1: Int,
        index1: Int
    ): Boolean {
        return false
    }


}
