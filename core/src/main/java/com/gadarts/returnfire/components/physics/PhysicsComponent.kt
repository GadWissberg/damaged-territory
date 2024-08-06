package com.gadarts.returnfire.components.physics

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.Collision
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.components.GameComponent

class PhysicsComponent : GameComponent(), Disposable {
    private val localInertia = Vector3()
    private var motionState: MotionState? = null
    lateinit var rigidBody: btRigidBody

    override fun reset() {

    }

    fun init(
        colShape: btCollisionShape,
        mass: Float,
        transform: Matrix4?,
        flag: Int,
    ) {
        if (transform != null) {
            motionState = MotionState()
            motionState!!.transformObject = transform
            motionState!!.setWorldTransform(transform)
        }
        calculateLocalInertia(colShape, mass)
        initializeBody(colShape, mass, transform, flag)
    }

    private fun initializeBody(
        colShape: btCollisionShape,
        mass: Float,
        transform: Matrix4?,
        flag: Int,
    ) {
        this.rigidBody = btRigidBody(mass, motionState, colShape, localInertia)
        if (transform != null) {
            this.rigidBody.worldTransform = transform
        }
        activateBody()
        this.rigidBody.collisionFlags =
            this.rigidBody.collisionFlags or flag or btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK
        rigidBody.angularFactor = Vector3(1F, 1F, 1F)
        rigidBody.friction = 1.5F
    }

    private fun activateBody() {
        rigidBody.setSleepingThresholds(1f, 1f)
        rigidBody.deactivationTime = 5f
        rigidBody.activate()
        rigidBody.activationState = Collision.DISABLE_DEACTIVATION
    }

    private fun calculateLocalInertia(
        collisionShape: btCollisionShape,
        mass: Float
    ) {
        if (mass == 0f) {
            localInertia.setZero()
        } else {
            collisionShape.calculateLocalInertia(mass, localInertia)
        }
    }

    override fun dispose() {
        motionState?.dispose()
        rigidBody.dispose()
    }

}
