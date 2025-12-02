package com.gadarts.shared.data.definitions.characters

import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.type.CharacterType

enum class TurretCharacterDefinition(
    val turretCorpseModelDefinitions: List<ModelDefinition>,
    private val hp: Float,
    private val baseModelDefinition: ModelDefinition,
    private val smokeEmissionRelativePosition: Vector3,
    private val gravity: Vector3,
    private val linearFactor: Vector3,
    private val corpseModelDefinitions: List<ModelDefinition>,
    private val isNonMoving: Boolean,
    private val mass: Float,
    private val textures: ElementTextures,
    private val turretTextures: ElementTextures,
    private val fuelConsumptionPace: Float = 0.0F,
    private val placeable: Boolean = false,
    private val linearDamping: Float = 0.0F,
    private val angularDamping: Float = 0.0F,
    private val deployable: Boolean = true,
    private val originPointAtBottom: Boolean = false,
) : CharacterDefinition {
    GUARD_TURRET_CANNON(
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
        placeable = true,
        deployable = false,
        textures = ElementTextures("guard_turret_base_texture", null),
        turretTextures = ElementTextures("guard_turret_cannon_texture", "guard_turret_cannon_dead_texture"),
    ),
    TANK(
        hp = 1F,
        baseModelDefinition = ModelDefinition.TANK_BODY,
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
        linearDamping = 0.9F,
        angularDamping = 0.99F,
        placeable = false,
        textures = ElementTextures("tank_body_texture", "tank_body_texture_dead"),
        turretTextures = ElementTextures("tank_turret_texture", "tank_turret_texture_destroyed"),
    ),
    JEEP(
        hp = 60F,
        baseModelDefinition = ModelDefinition.JEEP,
        smokeEmissionRelativePosition = Vector3.Zero,
        gravity = Vector3(0F, -10F, 0F),
        linearFactor = Vector3(1F, 1F, 1F),
        corpseModelDefinitions = listOf(ModelDefinition.JEEP_DESTROYED),
        turretCorpseModelDefinitions = listOf(),
        isNonMoving = false,
        mass = 10F,
        fuelConsumptionPace = 0.1F,
        placeable = false,
        linearDamping = 0.8F,
        angularDamping = 0.999F,
        originPointAtBottom = true,
        textures = ElementTextures("jeep_texture", "jeep_texture_dead"),
        turretTextures = ElementTextures("jeep_turret_base", null),
    );

    override fun isOriginPointAtBottom(): Boolean {
        return originPointAtBottom
    }

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

    override fun isDeployable(): Boolean {
        return deployable
    }

    override fun textures(): ElementTextures {
        return textures
    }

    fun turretTextures(): ElementTextures {
        return turretTextures
    }
}
