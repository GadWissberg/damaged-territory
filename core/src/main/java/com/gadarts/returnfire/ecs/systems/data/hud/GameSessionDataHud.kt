package com.gadarts.returnfire.ecs.systems.data.hud

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.console.ConsoleImpl

class GameSessionDataHud(
    val console: ConsoleImpl,
) :
    Disposable {
    val stage: Stage = Stage()
    lateinit var movementTouchpad: Touchpad
    lateinit var turretTouchpad: Touchpad
    lateinit var ui: Table

    override fun dispose() {
        stage.dispose()
    }

}
