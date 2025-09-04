package com.gadarts.shared.data.definitions

import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.type.CharacterType

enum class TurretCharacterDefinition(
    private val hp: Float,
    private val baseModelDefinition: ModelDefinition,
    private val smokeEmissionRelativePosition: Vector3,
    private val gravity: Vector3,
    private val linearFactor: Vector3,
    private val corpseModelDefinitions: List<ModelDefinition>,
    val turretCorpseModelDefinitions: List<ModelDefinition>,
    private val isNonMoving: Boolean,
    private val mass: Float,
    private val fuelConsumptionPace: Float = 0.0F,
    private val placeable: Boolean = false,
    private val linearDamping: Float = 0.0F,
    private val angularDamping: Float = 0.0F,
    val separateTextureForDeadTurret: Boolean = false,
) : CharacterDefinition {
    TURRET_CANNON(
        hp = 75F,
        baseModelDefinition = ModelDefinition.TURRET_BASE,
        smokeEmissionRelativePosition = Vector3(0F, 2F, 0F),
        gravity = Vector3.Zero,
        linearFactor = Vector3.Zero,
        corpseModelDefinitions = listOf(
            ModelDefinition.TURRET_CANNON_DEAD_0,
            ModelDefinition.TURRET_CANNON_DEAD_1,
        ),
        turretCorpseModelDefinitions = listOf(
            ModelDefinition.TURRET_CANNON_DEAD_0,
            ModelDefinition.TURRET_CANNON_DEAD_1,
        ),
        isNonMoving = true,
        mass = 0F,
        placeable = true
    ),
    TANK(
        hp = 275F,
        baseModelDefinition = ModelDefinition.TANK_BODY,
        smokeEmissionRelativePosition = Vector3.Zero,
        gravity = Vector3(0F, -10F, 0F),
        linearFactor = Vector3(1F, 1F, 1F),
        listOf(ModelDefinition.TANK_BODY_DESTROYED),
        listOf(
            ModelDefinition.TANK_TURRET_DESTROYED,
        ),
        isNonMoving = false,
        mass = 10F,
        fuelConsumptionPace = 0.1F,
        linearDamping = 0.9F,
        angularDamping = 0.99F,
        separateTextureForDeadTurret = true
    ),
    JEEP(
        hp = 65F,
        baseModelDefinition = ModelDefinition.JEEP,
        smokeEmissionRelativePosition = Vector3.Zero,
        gravity = Vector3(0F, -10F, 0F),
        linearFactor = Vector3(1F, 1F, 1F),
        corpseModelDefinitions = listOf(ModelDefinition.TANK_BODY_DESTROYED),
        turretCorpseModelDefinitions = listOf(
            ModelDefinition.TANK_TURRET_DESTROYED,
        ),
        isNonMoving = false,
        mass = 10F,
        fuelConsumptionPace = 0.1F,
        placeable = false,
        linearDamping = 0.8F,
        angularDamping = 0.999F,
    );

    override fun isUseSeparateTransformObjectForPhysics(): Boolean {
        return false
    }

    override fun isFlyer(): Boolean {
        return false
    }

    override fun getModelDefinition(): ModelDefinition {
        return baseModelDefinition
    }

    override fun getMovementHeight(): Float {
        return 0.1F
    }

    override fun getLinearFactor(output: Vector3): Vector3 {
        return output.set(linearFactor)
    }

    override fun getAngularFactor(output: Vector3): Vector3 {
        return output.setZero()
    }

    override fun getGravity(output: Vector3): Vector3 {
        return output.set(gravity)
    }

    override fun getSmokeEmissionRelativePosition(output: Vector3): Vector3 {
        return output.set(smokeEmissionRelativePosition)
    }

    override fun getHP(): Float {
        return hp
    }

    override fun getName(): String {
        return name
    }

    override fun getCharacterType(): CharacterType {
        return CharacterType.TURRET
    }

    override fun isRandomizeRotation(): Boolean {
        return false
    }

    override fun getScale(): Float {
        return 1F
    }

    override fun isGibable(): Boolean {
        return false
    }

    override fun getCorpseModelDefinitions(): List<ModelDefinition> {
        return corpseModelDefinitions
    }

    override fun isMarksNodeAsBlocked(): Boolean {
        return isNonMoving
    }

    override fun isConsumingFuelOnIdle(): Boolean {
        return false
    }

    override fun getFuelConsumptionPace(): Float {
        return fuelConsumptionPace
    }

    override fun isPlaceable(): Boolean {
        return placeable
    }

    override fun getLinearDamping(): Float {
        return linearDamping
    }

    override fun getAngularDamping(): Float {
        return angularDamping
    }

    override fun getFriction(): Float {
        return 0F
    }

    override fun getMass(): Float {
        return mass
    }
}
