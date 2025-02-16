package com.gadarts.returnfire.screens.hangar

import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider

class HangarSceneLightingData {
    val shadowBatch = ModelBatch(DepthShaderProvider())
    val environment: Environment by lazy { Environment() }
    val shadowLight: DirectionalShadowLight by lazy {
        DirectionalShadowLight(
            2056,
            2056,
            30f,
            30f,
            .1f,
            150f
        )
    }

}
