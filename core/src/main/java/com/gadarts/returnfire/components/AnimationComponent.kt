package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.utils.AnimationController
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.TimeUtils

class AnimationComponent(modelInstance: ModelInstance) : Component {
    val initialSpeed: Float

    var nextSpeedChangeTime: Long = 0
        private set
    val animationController: AnimationController = AnimationController(modelInstance)

    init {
        animationController.setAnimation(modelInstance.animations[0].id, -1)
        initialSpeed = animationController.current.speed
        animationController.allowSameAnimation = true
    }

    fun randomizeSpeed() {
        animationController.current.speed = initialSpeed * MathUtils.random(0.1F, 2.5F)
        nextSpeedChangeTime = MathUtils.random(1000, 5000) + TimeUtils.millis()
    }
}
