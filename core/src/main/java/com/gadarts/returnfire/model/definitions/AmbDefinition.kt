package com.gadarts.returnfire.model.definitions

import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.model.ElementType

enum class AmbDefinition(
    private val modelDefinition: ModelDefinition,
    private val randomizeRotation: Boolean = false,
    val collisionFlags: Int = CollisionFlags.CF_STATIC_OBJECT,
    val placeInMiddleOfCell: Boolean = true,
    private val scale: Float = 1F,
    val hp: Int = -1,
    val mass: Float = 0F,
    val flyingPart: ModelDefinition? = null,
    val minFlyingParts: Int = 2,
    val maxFlyingParts: Int = 4,
    val hasDeathSequence: Boolean = false,
    val destructionSound: SoundDefinition? = null
) : ElementDefinition {
    PALM_TREE(
        modelDefinition = ModelDefinition.PALM_TREE,
        randomizeRotation = true,
        collisionFlags = CollisionFlags.CF_KINEMATIC_OBJECT,
        hp = 1,
        mass = 1F
    ),
    WATCH_TOWER(ModelDefinition.WATCH_TOWER),
    BUILDING_FLAG(ModelDefinition.BUILDING_FLAG),
    FLAG(ModelDefinition.FLAG),
    BASE_BROWN(
        modelDefinition = ModelDefinition.PIT,
        collisionFlags = -1,
        placeInMiddleOfCell = false
    ),
    BASE_GREEN(
        modelDefinition = ModelDefinition.PIT,
        collisionFlags = -1,
        placeInMiddleOfCell = false
    ),
    ROCK_BIG(
        modelDefinition = ModelDefinition.ROCK_BIG,
        hp = 3,
        flyingPart = ModelDefinition.ROCK_PART,
        destructionSound = SoundDefinition.ROCKS
    ),
    ROCK_MED(
        modelDefinition = ModelDefinition.ROCK_MED,
        hp = 2,
        flyingPart = ModelDefinition.ROCK_PART,
        destructionSound = SoundDefinition.ROCKS
    ),
    ROCK_SMALL(
        modelDefinition = ModelDefinition.ROCK_SMALL,
        hp = 1,
        flyingPart = ModelDefinition.ROCK_PART,
        destructionSound = SoundDefinition.ROCKS
    ),
    BUILDING_0(
        modelDefinition = ModelDefinition.BUILDING_0,
        collisionFlags = CollisionFlags.CF_KINEMATIC_OBJECT,
        mass = 1F,
        hp = 4,
        flyingPart = ModelDefinition.ROCK_PART,
        hasDeathSequence = true,
        destructionSound = SoundDefinition.ROCKS
    ),
    ANTENNA(
        modelDefinition = ModelDefinition.ANTENNA,
        collisionFlags = CollisionFlags.CF_KINEMATIC_OBJECT,
        mass = 0F,
        hp = 2,
        flyingPart = ModelDefinition.ANTENNA_PART,
        minFlyingParts = 3,
        maxFlyingParts = 5,
        destructionSound = SoundDefinition.METAL_BEND
    );


    override fun getModelDefinition(): ModelDefinition {
        return modelDefinition
    }

    override fun getName(): String {
        return name
    }

    override fun getType(): ElementType {
        return ElementType.AMB
    }

    override fun isRandomizeRotation(): Boolean {
        return randomizeRotation
    }

    override fun getScale(): Float {
        return scale
    }
}
