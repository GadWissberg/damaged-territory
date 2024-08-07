package com.gadarts.returnfire.systems.physics

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.physics.bullet.collision.ContactListener
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import com.gadarts.returnfire.components.ComponentsMapper


class GameContactListener(private val dispatcher: MessageDispatcher) : ContactListener() {
//    override fun onContactAdded(
//        colObj0: btCollisionObject,
//        partId0: Int,
//        index0: Int,
//        colObj1: btCollisionObject,
//        partId1: Int,
//        index1: Int
//    ): Boolean {
//        PhysicsCollisionEventData.set(colObj0, colObj1)
//        dispatcher.dispatchMessage(SystemEvents.PHYSICS_COLLISION.ordinal)
//        colObj0.userData
//        return true
//    }

    override fun onContactStarted(
        colObj0: btCollisionObject?,
        match0: Boolean,
        colObj1: btCollisionObject?,
        match1: Boolean
    ) {
        if (ComponentsMapper.amb.has(colObj0!!.userData as Entity) || ComponentsMapper.amb.has(colObj1!!.userData as Entity)) {
            Gdx.app.log("!", "!")
        }
    }


}

