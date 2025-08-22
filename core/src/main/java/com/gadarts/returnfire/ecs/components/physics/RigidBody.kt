package com.gadarts.returnfire.ecs.components.physics

import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState
import com.gadarts.returnfire.ecs.systems.data.pools.RigidBodyPool

class RigidBody(
    val mass: Float,
    motionState: btMotionState?,
    collisionShape: btCollisionShape?,
    localInertia: Vector3?,
    val rigidBodyPool: RigidBodyPool?
) :
    btRigidBody(mass, motionState, collisionShape, localInertia)
