package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.model.GameMap

class GameSessionData(assetsManager: GameAssetManager, val runsOnMobile: Boolean) :
    Disposable {
    val gameSessionPhysicsData = GameSessionPhysicsData()
    val currentMap: GameMap =
        assetsManager.getAll(GameMap::class.java, com.badlogic.gdx.utils.Array())[0]
    val gameSessionDataEntities = GameSessionDataEntities()
    val camera: PerspectiveCamera = PerspectiveCamera(
        FOV,
        Gdx.graphics.width.toFloat(),
        Gdx.graphics.height.toFloat()
    )
    val gameSessionDataHud = GameSessionDataHud(assetsManager)
    val gameSessionDataPools = GameSessionDataPools(assetsManager)
    val gameSessionDataRender = GameSessionDataRender()

    companion object {
        const val FOV = 67F
        const val UI_TABLE_NAME = "ui_table"
        const val SPARK_FORWARD_BIAS = 0.55F
        const val SPARK_HEIGHT_BIAS = 0.37F
        const val REGION_SIZE = 10
    }

    override fun dispose() {
        gameSessionDataRender.dispose()
        gameSessionDataHud.dispose()
        gameSessionPhysicsData.dispose()
    }
}