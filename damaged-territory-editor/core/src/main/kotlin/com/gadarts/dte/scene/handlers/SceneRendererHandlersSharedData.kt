package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.ModelInstance

class SceneRendererHandlersSharedData {
    lateinit var camera: PerspectiveCamera
    val modelInstances = mutableListOf<ModelInstance>()

}
