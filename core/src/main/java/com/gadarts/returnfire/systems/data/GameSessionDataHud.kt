package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.managers.GameAssetManager

class GameSessionDataHud(assetsManager: GameAssetManager, val console: ConsoleImpl) : Disposable {
    val stage: Stage = Stage()
    val movementTouchpad: Touchpad = createTouchpad(assetsManager)
    val turretTouchpad: Touchpad = createTouchpad(assetsManager)

    private fun createTouchpad(assetsManager: GameAssetManager) = Touchpad(
        15F,
        Touchpad.TouchpadStyle(
            TextureRegionDrawable(
                assetsManager.getTexture(assetsManager.getTexturesDefinitions().definitions["joystick"]!!)
            ),
            TextureRegionDrawable(
                assetsManager.getTexture(assetsManager.getTexturesDefinitions().definitions["joystick_center"]!!),
            )
        )
    )

    override fun dispose() {
        stage.dispose()
    }

}
