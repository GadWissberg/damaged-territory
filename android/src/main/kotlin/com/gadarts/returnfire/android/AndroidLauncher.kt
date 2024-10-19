package com.gadarts.returnfire.android

import android.os.Bundle
import android.view.WindowManager
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.gadarts.returnfire.DamagedTerritory

class AndroidLauncher : AndroidApplication() {
    private val damagedTerritory = DamagedTerritory(true, 120)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val androidApplicationConfiguration = AndroidApplicationConfiguration()
        androidApplicationConfiguration.numSamples = 2
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        initialize(damagedTerritory, androidApplicationConfiguration.apply {
            useImmersiveMode = true
        })
    }

}
