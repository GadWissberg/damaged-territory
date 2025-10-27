package com.gadarts.returnfire.ecs.systems.data

import com.badlogic.ashley.core.PooledEngine
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.ecs.systems.ai.logic.goals.AiOpponentGoal
import com.gadarts.returnfire.ecs.systems.data.hud.GameSessionDataHud
import com.gadarts.returnfire.ecs.systems.data.map.GameSessionDataMap
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.data.CharacterColor
import com.gadarts.shared.data.definitions.characters.CharacterDefinition

class GameSessionData(
    val runsOnMobile: Boolean,
    val fpsTarget: Int,
    var selectedCharacter: CharacterDefinition,
    val autoAim: Boolean,
    assetsManager: GameAssetManager,
    console: ConsoleImpl,
    engine: PooledEngine,
    opponentsData: Map<CharacterColor, OpponentData>
) :
    Disposable {
    val profilingData: GameSessionDataProfiling = GameSessionDataProfiling()
    val physicsData = GameSessionDataPhysics()
    val gamePlayData = GameSessionDataGameplay(opponentsData, assetsManager, engine)
    val mapData = GameSessionDataMap(assetsManager)
    val hudData =
        GameSessionDataHud(console)
    val renderData = GameSessionDataRender()
    var aiOpponentGoal: AiOpponentGoal? = null


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
        dispose()
    }
}
