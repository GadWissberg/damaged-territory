package com.gadarts.returnfire.ecs.systems.player

interface PlayerSystem {
    fun initInputMethod()

    companion object {
        const val AUTO_AIM_HEIGHT = 8F
    }
}
