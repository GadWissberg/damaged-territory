package com.gadarts.returnfire.ecs.systems.data.pools

import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.Pool
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.systems.player.GameModelInstancePool
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.SharedUtils
import com.gadarts.shared.assets.definitions.model.ModelDefinition

class GameSessionDataPools(
    private val assetsManager: GameAssetManager,
) : Disposable {
    private val floorModel = createFloorModel()

    val gameModelInstancePools = ModelDefinition.entries
        .filter { it.physicsData.pooledObjectPhysicalDefinition != null }
        .associateWith { createPool(it) }

    val particleEffectsPools = ParticleEffectsPools(assetsManager)
    val rigidBodyPools = RigidBodyPools(assetsManager, RigidBodyFactory())
    val groundBlastPool = object : Pool<GameModelInstance>() {
        override fun newObject(): GameModelInstance {
            return GameModelInstance(ModelInstance(floorModel), null)
        }
    }

    private fun createFloorModel(): Model {
        val builder = ModelBuilder()
        builder.begin()
        SharedUtils.createFlatMesh(builder, "floor", 0.5F, null, 0F)
        return builder.end()
    }

    private fun createPool(modelDefinition: ModelDefinition) =
        GameModelInstancePool(
            assetsManager.getAssetByDefinition(modelDefinition),
            modelDefinition
        )

    override fun dispose() {
        floorModel.dispose()
        rigidBodyPools.pools.forEach {
            it.value.clear()
        }
    }
}
