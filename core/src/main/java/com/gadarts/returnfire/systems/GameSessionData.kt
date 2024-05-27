package com.gadarts.returnfire.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.ModelDefinition
import com.gadarts.returnfire.assets.TexturesDefinitions
import com.gadarts.returnfire.model.GameMap
import com.gadarts.returnfire.systems.player.BulletsPool

class GameSessionData(assetsManager: GameAssetManager) : Disposable {
    val currentMap: GameMap =
        assetsManager.getAll(GameMap::class.java, com.badlogic.gdx.utils.Array())[0]
    lateinit var player: Entity
    lateinit var entitiesAcrossRegions: Array<Array<MutableList<Entity>?>>
    val camera: PerspectiveCamera = PerspectiveCamera(
        FOV,
        Gdx.graphics.width.toFloat(),
        Gdx.graphics.height.toFloat()
    )
    val stage: Stage = Stage()
    lateinit var modelCache: ModelCache
    val touchpad: Touchpad = Touchpad(
        15F,
        Touchpad.TouchpadStyle(
            TextureRegionDrawable(
                assetsManager.getAssetByDefinition(
                    TexturesDefinitions.JOYSTICK
                )
            ),
            TextureRegionDrawable(assetsManager.getAssetByDefinition(TexturesDefinitions.JOYSTICK_CENTER))
        )
    )
    val priBulletsPool: BulletsPool = BulletsPool(
        assetsManager.getAssetByDefinition(ModelDefinition.BULLET),
        BoundingBox(assetsManager.getCachedBoundingBox(ModelDefinition.BULLET))
    )
    val secBulletsPool: BulletsPool = BulletsPool(
        assetsManager.getAssetByDefinition(ModelDefinition.MISSILE),
        BoundingBox(assetsManager.getCachedBoundingBox(ModelDefinition.MISSILE))
    )

    companion object {
        const val FOV = 67F
        const val UI_TABLE_NAME = "ui_table"
        const val SPARK_FORWARD_BIAS = 0.55F
        const val SPARK_HEIGHT_BIAS = 0.37F
        const val REGION_SIZE = 10
    }

    override fun dispose() {
        modelCache.dispose()
        stage.dispose()
    }
}
