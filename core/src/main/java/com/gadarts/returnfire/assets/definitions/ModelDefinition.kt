package com.gadarts.returnfire.assets.definitions

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.*
import com.gadarts.returnfire.model.definitions.PooledObjectPhysicalDefinition


enum class ModelDefinition(
    fileNames: Int = 1,
    val boundingBoxScale: Vector3 = Vector3(1F, 1F, 1F),
    val boundingBoxBias: Vector3 = Vector3.Zero,
    val pooledObjectPhysicalDefinition: PooledObjectPhysicalDefinition? = null,
    val physicalShapeCreator: PhysicalShapeCreator? = null,
    val centerOfMass: Vector3 = Vector3.Zero,
    val separateModelForShadow: Boolean = false,
    val origin: Vector3 = Vector3.Zero,
    val decal: String? = null
) :
    AssetDefinition<Model> {

    APACHE(centerOfMass = Vector3(0F, -0.2F, 0F), separateModelForShadow = true),
    APACHE_DEAD,
    APACHE_DEAD_FRONT,
    APACHE_DEAD_BACK,
    BULLET(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.BULLET_FLAT),
    CANNON_BULLET(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.BULLET_FLAT),
    MISSILE(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.MISSILE),
    PALM_TREE(
        fileNames = 2,
        boundingBoxScale = Vector3(0.25F, 1F, 0.25F),
        physicalShapeCreator = PalmTreePhysicalShapeCreator
    ),
    PALM_TREE_LEAF(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.PALM_TREE_LEAF),
    PALM_TREE_PART(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.PALM_TREE_PART),
    WATCH_TOWER(physicalShapeCreator = WatchTowerPhysicalShapeCreator),
    WATCH_TOWER_DESTROYED(physicalShapeCreator = WatchTowerDestroyedPhysicalShapeCreator),
    WATCH_TOWER_DESTROYED_PART(fileNames = 2, physicalShapeCreator = WatchTowerDestroyedPartPhysicalShapeCreator),
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
    TURRET_BASE(centerOfMass = Vector3(0F, 0.5F, -0F), physicalShapeCreator = TurretBasePhysicalShapeCreator),
    MACHINE_GUN_SPARK,
    CANNON_SPARK,
    FLYING_PART(boundingBoxScale = Vector3(0.5F, 0.5F, 0.5F), fileNames = 3),
    FLYING_PART_SMALL(boundingBoxScale = Vector3(0.25F, 0.25F, 0.25F)),
    TANK_BODY,
    TANK_TURRET,
    TANK_CANNON,
    TANK_MISSILE_LAUNCHER,
    TANK_CANNON_BULLET(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.TANK_CANNON_BULLET),
    SCENE,
    HOOK,
    FAN,
    CEILING,
    STAGE,
    PROPELLER,
    PIT,
    PIT_DOOR,
    ROCK_BIG(physicalShapeCreator = AutomaticShapeCreator),
    ROCK_MED(physicalShapeCreator = AutomaticShapeCreator),
    ROCK_SMALL(physicalShapeCreator = AutomaticShapeCreator),
    ROCK_PART,
    ROCK_PART_BIG,
    BUILDING_0(physicalShapeCreator = Building0ShapeCreator),
    BUILDING_0_DESTROYED(fileNames = 2, physicalShapeCreator = Building0DestroyedShapeCreator),
    BUILDING_0_PART(fileNames = 2, pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.BUILDING_0_PART),
    ANTENNA(physicalShapeCreator = AntennaShapeCreator),
    ANTENNA_DESTROYED_BASE(physicalShapeCreator = AntennaDestroyedBaseShapeCreator),
    ANTENNA_DESTROYED_BODY(
        physicalShapeCreator = AntennaDestroyedBodyShapeCreator,
    ),
    ANTENNA_PART(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.ANTENNA_PART),
    STREET_LIGHT(physicalShapeCreator = StreetLightPhysicalShapeCreator, origin = Vector3(0F, -0.7F, 0F)),
    FENCE(physicalShapeCreator = FencePhysicalShapeCreator, decal = "fence"),
    FENCE_PART(physicalShapeCreator = FencePartPhysicalShapeCreator),
    DESTROYED_BUILDING(fileNames = 2, physicalShapeCreator = Building0DestroyedShapeCreator), ;

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

object AutomaticShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        return btConvexHullShape()
    }

}

object PalmTreePhysicalShapeCreator : PhysicalShapeCreator {
    private val auxMatrix = Matrix4()
    override fun create(): btCollisionShape {
        val shape = btCompoundShape()
        val btBoxShape = btBoxShape(Vector3(0.06F, 0.5F, 0.06F))
        shape.addChildShape(
            auxMatrix.idt().translate(0F, 0.4F, 0F), btBoxShape
        )
        return shape
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

object Building0ShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        val shape = btCompoundShape()
        val btBoxShape = btBoxShape(Vector3(1.5F, 0.875F, 1F))
        shape.addChildShape(
            Matrix4().idt().translate(0F, 0.875F, 0F), btBoxShape
        )
        return shape
    }

}

object Building0DestroyedShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        val shape = btCompoundShape()
        val btBoxShape = btBoxShape(Vector3(2F, 0.25F, 1F))
        shape.addChildShape(
            Matrix4().idt().translate(0F, 0.125F, 0F), btBoxShape
        )
        return shape
    }

}

object AntennaShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        val shape = btCompoundShape()
        val btBoxShape = btBoxShape(Vector3(0.125F, 2.125F, 0.125F))
        shape.addChildShape(
            Matrix4().idt().translate(0F, 2.125F, 0F), btBoxShape
        )
        return shape
    }

}

object AntennaDestroyedBaseShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        val shape = btCompoundShape()
        val btBoxShape = btBoxShape(Vector3(0.125F, 0.5F, 0.125F))
        shape.addChildShape(
            Matrix4().idt().translate(0F, 0.5F, 0F), btBoxShape
        )
        return shape
    }

}

object AntennaDestroyedBodyShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        val btBoxShape = btBoxShape(Vector3(0.125F, 2.25F, 0.125F))
        return btBoxShape
    }

}

object WatchTowerPhysicalShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        val shape = btCompoundShape()
        val btBoxShape = btBoxShape(Vector3(0.25F, 0.9F, 0.25F))
        shape.addChildShape(
            Matrix4().idt().translate(0F, 0.9F, 0F), btBoxShape
        )
        return shape
    }

}

object WatchTowerDestroyedPhysicalShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        return btBoxShape(Vector3(0.35F, 0.25F, 0.4F))
    }

}

object WatchTowerDestroyedPartPhysicalShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        return btBoxShape(Vector3(0.1F, 0.15F, 0.17F))
    }

}

object StreetLightPhysicalShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        val shape = btCompoundShape()
        val body = btConeShape(0.03F, 0.8F)
        val head = btBoxShape(Vector3(0.15F, 0.05F, 0.05F))
        shape.addChildShape(
            Matrix4().idt(), body,
        )
        shape.addChildShape(
            Matrix4().idt().translate(0.17F, 0.8F, 0F), head
        )
        return shape
    }

}

object FencePhysicalShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        val shape = btCompoundShape()
        val btBoxShape = btBoxShape(Vector3(0.03F, 0.5F, 0.5F))
        shape.addChildShape(
            Matrix4().idt().translate(0F, 0.5F, 0F), btBoxShape
        )
        return shape
    }

}

object FencePartPhysicalShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        return btBoxShape(Vector3(0.03F, 0.03F, 0.5F))
    }

}
