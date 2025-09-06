package com.gadarts.returnfire.ecs.systems.data

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.ecs.systems.data.hud.GameSessionDataHud
import com.gadarts.returnfire.ecs.systems.data.map.GameSessionDataMap
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.data.definitions.CharacterDefinition

class GameSessionData(
    assetsManager: GameAssetManager,
    val runsOnMobile: Boolean,
    val fpsTarget: Int,
    console: ConsoleImpl,
    var selectedCharacter: CharacterDefinition,
    val autoAim: Boolean,
    engine: PooledEngine
) :
    Disposable {
    val profilingData: GameSessionDataProfiling = GameSessionDataProfiling()
    val physicsData = GameSessionDataPhysics()
    val gamePlayData = GameSessionDataGameplay(assetsManager, engine)
    val mapData = GameSessionDataMap(assetsManager)
    val hudData =
        GameSessionDataHud(console, assetsManager, mapData.loadedMap, gamePlayData)
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
