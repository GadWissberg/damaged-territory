package com.gadarts.shared.assets.definitions.model

import com.badlogic.gdx.assets.AssetLoaderParameters
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.bullet.collision.*
import com.gadarts.shared.assets.definitions.AssetDefinition
import com.gadarts.shared.assets.definitions.PhysicalShapeCreator
import com.gadarts.shared.data.definitions.PooledObjectPhysicalDefinition


enum class ModelDefinition(
    val boundingBoxData: ModelDefinitionBoundingBoxData = ModelDefinitionBoundingBoxData(
        Vector3(1F, 1F, 1F),
        Vector3.Zero
    ),
    val fileNames: Int = 1,
    val physicsData: ModelDefinitionPhysicsData = ModelDefinitionPhysicsData(
        pooledObjectPhysicalDefinition = null,
        physicalShapeCreator = null,
        centerOfMass = Vector3.Zero
    ),
    val separateModelForShadow: Boolean = false,
    val origin: Vector3 = Vector3.Zero,
    val decal: String? = null,
    val loopAnimation: Boolean = false,
    val mainMaterialIndex: Int = 0,
) :
    AssetDefinition<Model> {

    TILE_FLAT,
    TILE_BUMPY(fileNames = 3),
    APACHE(
        physicsData = ModelDefinitionPhysicsData(centerOfMass = Vector3(0F, -0.2F, 0F)),
        separateModelForShadow = true
    ),
    APACHE_DEAD,
    APACHE_DEAD_FRONT(mainMaterialIndex = 1),
    APACHE_DEAD_BACK(mainMaterialIndex = 1),
    BULLET(physicsData = ModelDefinitionPhysicsData(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.BULLET_FLAT)),
    CANNON_BULLET(physicsData = ModelDefinitionPhysicsData(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.BULLET_FLAT)),
    MISSILE(physicsData = ModelDefinitionPhysicsData(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.MISSILE)),
    PALM_TREE(
        fileNames = 2,
        boundingBoxData = ModelDefinitionBoundingBoxData(Vector3(0.25F, 1F, 0.25F)),
    ),
    PALM_TREE_LEAF(physicsData = ModelDefinitionPhysicsData(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.PALM_TREE_LEAF)),
    PALM_TREE_PART(physicsData = ModelDefinitionPhysicsData(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.PALM_TREE_PART)),
    WATCH_TOWER,
    WATCH_TOWER_DESTROYED,
    WATCH_TOWER_DESTROYED_PART(
        fileNames = 2,
        physicsData = ModelDefinitionPhysicsData(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.WATCH_TOWER_DESTROYED_PART)
    ),
    FLAG(loopAnimation = true),
    FLAG_FLOOR,
    TURRET_CANNON(
        boundingBoxData = ModelDefinitionBoundingBoxData(Vector3(0.4F, 1F, 1F), Vector3(0.2F, 0F, 0F)),
        separateModelForShadow = true
    ),
    TURRET_CANNON_DEAD_0(
        boundingBoxData = ModelDefinitionBoundingBoxData(
            Vector3(0.4F, 1F, 1F),
            Vector3(0.2F, 0F, 0F)
        )
    ),
    TURRET_CANNON_DEAD_1(
        boundingBoxData = ModelDefinitionBoundingBoxData(
            Vector3(0.4F, 1F, 1F),
            Vector3(0.2F, 0F, 0F)
        )
    ),
    TURRET_BASE(
        physicsData = ModelDefinitionPhysicsData(
            centerOfMass = Vector3(0F, 0.5F, -0F),
            physicalShapeCreator = TurretBasePhysicalShapeCreator
        )
    ),
    MACHINE_GUN_SPARK,
    CANNON_SPARK,
    FLYING_PART(
        boundingBoxData = ModelDefinitionBoundingBoxData(
            Vector3(0.5F, 0.5F, 0.5F),
        ), fileNames = 3
    ),
    FLYING_PART_SMALL(
        boundingBoxData = ModelDefinitionBoundingBoxData(
            Vector3(0.25F, 0.25F, 0.25F),
        )
    ),
    TANK_BODY,
    TANK_TURRET,
    TANK_CANNON,
    TANK_MISSILE_LAUNCHER,
    TANK_CANNON_BULLET(physicsData = ModelDefinitionPhysicsData(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.TANK_CANNON_BULLET)),
    TANK_BODY_DESTROYED,
    TANK_TURRET_DESTROYED,
    TANK_CANNON_DESTROYED,
    SCENE,
    HOOK,
    FAN,
    CEILING,
    STAGE,
    PROPELLER,
    PIT,
    PIT_DOOR,
    ROCK_BIG(physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = AutomaticShapeCreator)),
    ROCK_MED(physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = AutomaticShapeCreator)),
    ROCK_SMALL(physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = AutomaticShapeCreator)),
    ROCK_PART,
    ROCK_PART_BIG,
    BUILDING_0(physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = Building0ShapeCreator)),
    BUILDING_0_DESTROYED(
        fileNames = 2,
        physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = Building0DestroyedShapeCreator)
    ),
    BUILDING_0_PART(
        fileNames = 2,
        physicsData = ModelDefinitionPhysicsData(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.BUILDING_0_PART)
    ),
    ANTENNA(physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = AntennaShapeCreator)),
    ANTENNA_DESTROYED_BASE(physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = AntennaDestroyedBaseShapeCreator)),
    ANTENNA_DESTROYED_BODY(
        physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = AntennaDestroyedBodyShapeCreator),
    ),
    ANTENNA_PART(physicsData = ModelDefinitionPhysicsData(pooledObjectPhysicalDefinition = PooledObjectPhysicalDefinition.ANTENNA_PART)),
    STREET_LIGHT(
        physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = StreetLightPhysicalShapeCreator),
        origin = Vector3(0F, -0.7F, 0F)
    ),
    FENCE(physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = FencePhysicalShapeCreator), decal = "fence"),
    FENCE_PART(physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = FencePartPhysicalShapeCreator)),
    FENCE_DESTROYED_RIGHT,
    FENCE_DESTROYED_LEFT,
    DESTROYED_BUILDING(
        fileNames = 2,
        physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = Building0DestroyedShapeCreator)
    ),
    HANGAR(physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = HangarPhysicalShapeCreator)),
    HANGAR_DESTROYED(physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = HangarDestroyedPhysicalShapeCreator)),
    SIGN(
        physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = SignPhysicalShapeCreator),
        origin = Vector3(0F, -0.2F, 0F)
    ),
    SIGN_BIG(
        physicsData = ModelDefinitionPhysicsData(physicalShapeCreator = AutomaticShapeCreator),
        origin = Vector3(0F, -0.63F, 0F)
    ),
    JEEP,
    JEEP_DESTROYED,
    JEEP_GUN,
    JEEP_TURRET_BASE,
    JEEP_WHEEL;

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

object TurretBasePhysicalShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        val trapezoidShape = btConvexHullShape()
        val bottomWidth = 0.4F
        val height = 1.5F
        val topWidth = 0.3F
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

object HangarPhysicalShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        val shape = btCompoundShape()
        val btBoxShape = btBoxShape(Vector3(2F, 0.75F, 1.5F))
        shape.addChildShape(
            Matrix4().idt().translate(0F, 0.75F, 0F), btBoxShape
        )
        return shape
    }

}

object HangarDestroyedPhysicalShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        val shape = btCompoundShape()
        val btBoxShape = btBoxShape(Vector3(2F, 0.362F, 1.5F))
        shape.addChildShape(
            Matrix4().idt().translate(0F, 0.362F, 0F), btBoxShape
        )
        return shape
    }

}

object SignPhysicalShapeCreator : PhysicalShapeCreator {
    override fun create(): btCollisionShape {
        return btBoxShape(Vector3(0.04F, 0.2F, 0.04F))
    }

}
