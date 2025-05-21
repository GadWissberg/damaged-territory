package com.gadarts.returnfire.systems.data.hud

import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.managers.GameAssetManager

class GameSessionDataHud(val console: ConsoleImpl, assetsManager: GameAssetManager, tilesMapping: Array<CharArray>) :
    Disposable {
    val minimap = Minimap(tilesMapping, assetsManager)
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
