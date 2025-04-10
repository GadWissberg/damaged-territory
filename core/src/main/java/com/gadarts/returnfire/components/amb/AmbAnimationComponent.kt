package com.gadarts.returnfire.components.amb

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
        decideNextTime()
        animationController.allowSameAnimation = true
    }

    fun playRegular() {
        play(1F)
    }

    private fun play(speed: Float) {
        animationController.setAnimation(
            modelInstance.animations[0].id,
            1,
            speed,
            object : AnimationController.AnimationListener {
                override fun onEnd(p0: AnimationController.AnimationDesc?) {
                }

                override fun onLoop(p0: AnimationController.AnimationDesc?) {
                }

            }
        )
        decideNextTime()
    }


    private fun decideNextTime() {
        nextPlay = TimeUtils.millis() + MathUtils.random(5, 25) * 1000L
    }

    fun applyAffectedByExplosionAnimation() {
        play(4F)
    }

}
