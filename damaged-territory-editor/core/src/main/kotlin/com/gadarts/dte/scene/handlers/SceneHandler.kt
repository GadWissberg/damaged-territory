package com.gadarts.dte.scene.handlers

import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.utils.Disposable

interface SceneHandler : Disposable {
    fun update(parent: Table, deltaTime: Float)

}
