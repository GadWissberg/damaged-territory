package com.gadarts.returnfire.ecs.systems.physics

import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.physics.bullet.collision.ContactListener
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.data.PhysicsCollisionEventData


class GameContactListener(private val dispatcher: MessageDispatcher) : ContactListener() {

    override fun onContactStarted(
        colObj0: btCollisionObject,
        match0: Boolean,
        colObj1: btCollisionObject,
        match1: Boolean
    ) {
        PhysicsCollisionEventData.set(colObj0, colObj1)
        dispatcher.dispatchMessage(SystemEvents.PHYSICS_COLLISION.ordinal)
    }
}

