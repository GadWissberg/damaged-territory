package com.gadarts.returnfire.components

import com.badlogic.ashley.core.Component
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.utils.AnimationController
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.TimeUtils

class AmbAnimationComponent(private val modelInstance: ModelInstance) : Component {
    var nextPlay: Long = 0L
        private set

    val animationController: AnimationController = AnimationController(modelInstance)

    init {
        animationController.allowSameAnimation = true
    }

    fun play() {
        animationController.setAnimation(modelInstance.animations[0].id, 1)
        nextPlay = TimeUtils.millis() + MathUtils.random(25) * 1000L
    }

}
