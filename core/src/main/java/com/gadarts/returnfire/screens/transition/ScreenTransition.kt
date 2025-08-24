package com.gadarts.returnfire.screens.transition

interface ScreenTransition {
    val duration: Float
    fun render(alpha: Float, deltaTime: Float)
}
