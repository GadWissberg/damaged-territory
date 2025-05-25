package com.gadarts.returnfire.model.definitions

import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.assets.definitions.model.ModelDefinition
import com.gadarts.returnfire.model.ElementType
import com.gadarts.returnfire.systems.data.map.GameSessionDataMap.Companion.DROWNING_HEIGHT

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
    val flyingPartMinImpulse: Float = 1F,
    val flyingPartMaxImpulse: Float = 7F,
    val hasDeathSequence: Boolean = false,
    val destructionSound: SoundDefinition? = null,
    val corpse: ModelDefinition? = null,
    val collapseThreshold: Float = 0F,
    val corpseCollisionSound: SoundDefinition? = null,
    val corpsePartDestroyOnGroundImpact: Boolean = false,
    val stayOnDeath: Boolean = false,
    val destroyedByExplosiveOnly: Boolean = true,
    val marksNodeAsBlocked: Boolean = true,
    val forceSingleNodeForMarksNodeAsBlocked: Boolean = false,
    val drowningHeight: Float = DROWNING_HEIGHT
) : ElementDefinition {
    PALM_TREE(
        modelDefinition = ModelDefinition.PALM_TREE,
        randomizeRotation = true,
        collisionFlags = CollisionFlags.CF_KINEMATIC_OBJECT,
        hp = 1,
        mass = 1F,
        destroyedByExplosiveOnly = false,
        stayOnDeath = true,
        marksNodeAsBlocked = false
    ),
    WATCH_TOWER(
        modelDefinition = ModelDefinition.WATCH_TOWER,
        hp = 3,
        minFlyingParts = 3,
        maxFlyingParts = 5,
        flyingPart = ModelDefinition.FLYING_PART,
        destructionSound = SoundDefinition.ROCKS,
        corpseCollisionSound = SoundDefinition.ROCKS,
        corpsePartDestroyOnGroundImpact = true,
    ),
    BUILDING_FLAG(ModelDefinition.BUILDING_FLAG),
    FLAG(ModelDefinition.FLAG),
    BASE_BROWN(
        modelDefinition = ModelDefinition.PIT,
        collisionFlags = -1,
        placeInMiddleOfCell = false,
        marksNodeAsBlocked = false
    ),
    BASE_GREEN(
        modelDefinition = ModelDefinition.PIT,
        collisionFlags = -1,
        placeInMiddleOfCell = false,
        marksNodeAsBlocked = false
    ),
    ROCK_BIG(
        modelDefinition = ModelDefinition.ROCK_BIG,
        hp = 2,
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
        mass = 20F,
        hp = 4,
        flyingPart = ModelDefinition.ROCK_PART,
        hasDeathSequence = true,
        destructionSound = SoundDefinition.ROCKS,
        corpse = ModelDefinition.BUILDING_0_DESTROYED,
        collapseThreshold = 0.9F
    ),
    ANTENNA(
        modelDefinition = ModelDefinition.ANTENNA,
        hp = 2,
        flyingPart = ModelDefinition.ANTENNA_PART,
        minFlyingParts = 3,
        maxFlyingParts = 5,
        destructionSound = SoundDefinition.METAL_BEND,
        corpseCollisionSound = SoundDefinition.METAL_CRASH,
        forceSingleNodeForMarksNodeAsBlocked = true,
        drowningHeight = 2F
    ),
    STREET_LIGHT(
        modelDefinition = ModelDefinition.STREET_LIGHT,
        collisionFlags = CollisionFlags.CF_KINEMATIC_OBJECT,
        mass = 3F,
        hp = 1,
        stayOnDeath = true,
        destroyedByExplosiveOnly = false,
        destructionSound = SoundDefinition.METAL_CRASH,
        forceSingleNodeForMarksNodeAsBlocked = true,
        corpseCollisionSound = SoundDefinition.METAL_LIGHT
    ),
    RUINS(
        modelDefinition = ModelDefinition.DESTROYED_BUILDING
    ),
    FENCE(
        modelDefinition = ModelDefinition.FENCE,
        hp = 1,
        minFlyingParts = 2,
        maxFlyingParts = 3,
        flyingPart = ModelDefinition.FENCE_PART,
        destructionSound = SoundDefinition.FENCE_BREAK,
        destroyedByExplosiveOnly = false,
        flyingPartMinImpulse = 1F,
        flyingPartMaxImpulse = 4F,
        forceSingleNodeForMarksNodeAsBlocked = true
    ),
    HANGAR(
        modelDefinition = ModelDefinition.HANGAR,
        hp = 4,
        flyingPart = ModelDefinition.FLYING_PART,
        collisionFlags = CollisionFlags.CF_KINEMATIC_OBJECT,
        hasDeathSequence = true,
        mass = 20F,
        destructionSound = SoundDefinition.METAL_CRASH,
        corpse = ModelDefinition.HANGAR_DESTROYED,
        collapseThreshold = 0.5F
    ),
    SIGN(
        modelDefinition = ModelDefinition.SIGN,
        collisionFlags = CollisionFlags.CF_KINEMATIC_OBJECT,
        mass = 0.5F,
        hp = 1,
        stayOnDeath = true,
        destroyedByExplosiveOnly = false,
        destructionSound = SoundDefinition.METAL_CRASH
    ),
    SIGN_BIG(
        modelDefinition = ModelDefinition.SIGN_BIG,
        collisionFlags = CollisionFlags.CF_KINEMATIC_OBJECT,
        mass = 1F,
        hp = 1,
        stayOnDeath = true,
        destroyedByExplosiveOnly = false,
        destructionSound = SoundDefinition.METAL_CRASH
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

    override fun isMarksNodeAsBlocked(): Boolean {
        return marksNodeAsBlocked
    }
}
