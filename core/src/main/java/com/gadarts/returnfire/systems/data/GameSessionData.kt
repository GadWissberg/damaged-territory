package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.systems.data.pools.GameSessionDataPools
import com.gadarts.returnfire.systems.data.pools.RigidBodyFactory

class GameSessionData(
    assetsManager: GameAssetManager,
    rigidBodyFactory: RigidBodyFactory,
    val runsOnMobile: Boolean,
    val fpsTarget: Int,
    console: ConsoleImpl,
    val selected: CharacterDefinition
) :
    Disposable {
    val physicsData = GameSessionDataPhysics()
    val gameplayData = GameSessionDataGameplay()
    val mapData = GameSessionDataMap(assetsManager)
    val hudData = GameSessionDataHud(assetsManager, console)
    val pools by lazy { GameSessionDataPools(assetsManager, rigidBodyFactory) }
    val renderData = GameSessionDataRender()


    companion object {
        const val UI_TABLE_NAME = "ui_table"
        const val APACHE_SPARK_HEIGHT_BIAS = 0.4F
    }

    override fun dispose() {
        mapData.dispose()
        renderData.dispose()
        hudData.dispose()
        physicsData.dispose()
        pools.dispose()
    }

    fun finishSession() {
        gameplayData.sessionFinished = true
        dispose()
    }
}
