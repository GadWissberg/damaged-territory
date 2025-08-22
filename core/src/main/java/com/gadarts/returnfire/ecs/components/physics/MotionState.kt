package com.gadarts.returnfire.ecs.components.physics

import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState

class MotionState : btMotionState() {
    lateinit var transformObject: Matrix4

    override fun getWorldTransform(worldTrans: Matrix4) {
        worldTrans.set(transformObject)
    }

    override fun setWorldTransform(worldTrans: Matrix4) {
        transformObject.set(worldTrans)
    }
}
