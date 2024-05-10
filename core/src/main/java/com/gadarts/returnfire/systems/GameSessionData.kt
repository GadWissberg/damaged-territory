package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.TexturesDefinitions
import com.gadarts.returnfire.model.GameMap

class GameSessionData(assetsManager: GameAssetManager) : Disposable {
    val currentMap: GameMap
    lateinit var player: Entity
    var touchpad: Touchpad
    val camera: PerspectiveCamera = PerspectiveCamera(
        FOV,
        Gdx.graphics.width.toFloat(),
        Gdx.graphics.height.toFloat()
    )
    val stage: Stage = Stage()
    lateinit var modelCache: ModelCache

    init {
        val joystickTexture = assetsManager.getAssetByDefinition(TexturesDefinitions.JOYSTICK)
        val joystickDrawableTex = TextureRegionDrawable(joystickTexture)
        val joystickCenterTex =
            TextureRegionDrawable(assetsManager.getAssetByDefinition(TexturesDefinitions.JOYSTICK_CENTER))
        touchpad = Touchpad(
            DEAD_ZONE,
            Touchpad.TouchpadStyle(joystickDrawableTex, joystickCenterTex)
        )
        currentMap = assetsManager.getAll(GameMap::class.java, com.badlogic.gdx.utils.Array())[0]
    }


    companion object {
        const val DEAD_ZONE = 15F
        const val FOV = 67F
        const val UI_TABLE_NAME = "ui_table"
        const val SPARK_FORWARD_BIAS = 0.55F
        const val SPARK_HEIGHT_BIAS = 0.37F
        const val REGION_SIZE = 10.0
    }

    override fun dispose() {
        modelCache.dispose()
        stage.dispose()
    }
}
