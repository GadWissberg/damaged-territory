package com.gadarts.returnfire.systems.data

import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.model.definitions.CharacterDefinition
import com.gadarts.returnfire.systems.data.hud.GameSessionDataHud
import com.gadarts.returnfire.systems.data.map.GameSessionDataMap

class GameSessionData(
    assetsManager: GameAssetManager,
    val runsOnMobile: Boolean,
    val fpsTarget: Int,
    console: ConsoleImpl,
    val selected: CharacterDefinition,
    val autoAim: Boolean
) :
    Disposable {
    val profilingData: GameSessionDataProfiling = GameSessionDataProfiling()
    val physicsData = GameSessionDataPhysics()
    val gamePlayData = GameSessionDataGameplay(assetsManager)
    val mapData = GameSessionDataMap(assetsManager)
    val hudData = GameSessionDataHud(console, assetsManager, mapData.currentMap.tilesTexturesMap, gamePlayData)
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
        gamePlayData.dispose()
    }

    fun finishSession() {
        gamePlayData.sessionFinished = true
        dispose()
    }
}
