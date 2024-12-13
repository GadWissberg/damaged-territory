package com.gadarts.returnfire.systems.player

interface PlayerSystem {
    fun initInputMethod()
    fun onboard()

    companion object {
        const val AUTO_AIM_HEIGHT = 8F
    }
}
