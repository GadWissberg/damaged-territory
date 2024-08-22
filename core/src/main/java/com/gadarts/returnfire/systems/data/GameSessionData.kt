package com.gadarts.returnfire.systems.data

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.model.GameMap

class GameSessionData(
    assetsManager: GameAssetManager,
    val runsOnMobile: Boolean,
    val fpsTarget: Int
) :
    Disposable {
    lateinit var player: Entity
    val gameSessionDataPhysics = GameSessionDataPhysics()
    val currentMap: GameMap =
        assetsManager.getAll(GameMap::class.java, com.badlogic.gdx.utils.Array())[0]
    val gameSessionDataHud = GameSessionDataHud(assetsManager)
    val gameSessionDataPools = GameSessionDataPools(assetsManager)
    val gameSessionDataRender = GameSessionDataRender()

    companion object {
        const val FOV = 67F
        const val UI_TABLE_NAME = "ui_table"
        const val SPARK_HEIGHT_BIAS = 0.4F
    }

    override fun dispose() {
        gameSessionDataRender.dispose()
        gameSessionDataHud.dispose()
        gameSessionDataPhysics.dispose()
    }
}
