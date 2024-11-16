package com.gadarts.returnfire.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.collision.btConvexHullShape
import com.gadarts.returnfire.model.PooledObjectPhysicalDefinition


enum class ModelDefinition(
    fileNames: Int = 1,
    val boundingBoxScale: Vector3 = Vector3(1F, 1F, 1F),
    val boundingBoxBias: Vector3 = Vector3.Zero,
    val pooledObjectPhysicalDefinition: PooledObjectPhysicalDefinition? = null,
    val physicalShapeCreator: PhysicalShapeCreator? = null,
    val centerOfMass: Vector3 = Vector3.Zero,
    val separateModelForShadow: Boolean = false
) :
    AssetDefinition<Model> {

    APACHE(centerOfMass = Vector3(0F, -0.2F, 0F), separateModelForShadow = true),
    BULLET(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.BULLET),
    CANNON_BULLET(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.BULLET),
    MISSILE(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.BULLET),
    PALM_TREE(fileNames = 3, boundingBoxScale = Vector3(0.25F, 1F, 0.25F), separateModelForShadow = true),
    WATCH_TOWER,
    BUILDING_FLAG,
    BUILDING_FLAG_DESTROYED,
    FLAG,
    TURRET_CANNON(
        boundingBoxScale = Vector3(0.4F, 1F, 1F),
        boundingBoxBias = Vector3(0.2F, 0F, 0F),
        separateModelForShadow = true
    ),
    TURRET_CANNON_DEAD_0(boundingBoxScale = Vector3(0.4F, 1F, 1F), boundingBoxBias = Vector3(0.2F, 0F, 0F)),
    TURRET_CANNON_DEAD_1(boundingBoxScale = Vector3(0.4F, 1F, 1F), boundingBoxBias = Vector3(0.2F, 0F, 0F)),
    TURRET_BASE(physicalShapeCreator = TurretBasePhysicalShapeCreator),
    MACHINE_GUN_SPARK,
    CANNON_SPARK,
    FLYING_PART(boundingBoxScale = Vector3(0.5F, 0.5F, 0.5F), fileNames = 3),
    TANK_BODY,
    TANK_TURRET,
    TANK_CANNON,
    TANK_CANNON_BULLET(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.TANK_CANNON_BULLET),
    SCENE;

    private val pathFormat = "models/%s.g3dj"
    private val paths = ArrayList<String>()
    val shadowsPaths = ArrayList<String>()

    init {
        initializePaths(pathFormat, getPaths(), fileNames)
        if (separateModelForShadow) {
            initializePaths(pathFormat, shadowsPaths, fileNames, "_shadow")
        }
    }

    override fun getPaths(): ArrayList<String> {
        return paths
    }

    override fun getParameters(): AssetLoaderParameters<Model>? {
        return null
    }

    override fun getClazz(): Class<Model> {
        return Model::class.java
    }

    override fun getDefinitionName(): String {
        return name
    }

}

object TurretBasePhysicalShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        val trapezoidShape = btConvexHullShape()
        val bottomWidth = 0.5F
        val height = 1.5F
        val topWidth = 0.4F
        val vertices = arrayOf(
            Vector3(-bottomWidth, 0f, -bottomWidth),
            Vector3(bottomWidth, 0f, -bottomWidth),
            Vector3(bottomWidth, 0f, bottomWidth),
            Vector3(-bottomWidth, 0f, bottomWidth),
            Vector3(-topWidth, height, -topWidth),
            Vector3(topWidth, height, -topWidth),
            Vector3(topWidth, height, topWidth),
            Vector3(-topWidth, height, topWidth),
        )
        for (vertex in vertices) {
            trapezoidShape.addPoint(vertex)
        }
        return trapezoidShape
    }

}
