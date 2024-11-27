package com.gadarts.returnfire.systems.data

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Pool
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.model.CharacterDefinition
import com.gadarts.returnfire.model.GameMap
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
    lateinit var player: Entity
    val gameSessionDataPhysics = GameSessionDataPhysics()
    val currentMap: GameMap =
        assetsManager.getAll(GameMap::class.java, com.badlogic.gdx.utils.Array())[0]
    val gameSessionDataHud = GameSessionDataHud(assetsManager, console)
    val pools by lazy { GameSessionDataPools(assetsManager, rigidBodyFactory) }
    val renderData = GameSessionDataRender()
    lateinit var tilesEntities: Array<Array<Entity?>>
    var sessionFinished: Boolean = false
        private set

    lateinit var floorModel: Model
    val groundBlastPool = object : Pool<GameModelInstance>() {
        override fun newObject(): GameModelInstance {
            return GameModelInstance(ModelInstance(floorModel), null)
        }
    }

    companion object {
        const val UI_TABLE_NAME = "ui_table"
        const val APACHE_SPARK_HEIGHT_BIAS = 0.4F
    }

    override fun dispose() {
        floorModel.dispose()
        renderData.dispose()
        gameSessionDataHud.dispose()
        gameSessionDataPhysics.dispose()
    }

    fun finishSession() {
        sessionFinished = true
        dispose()
    }
}
