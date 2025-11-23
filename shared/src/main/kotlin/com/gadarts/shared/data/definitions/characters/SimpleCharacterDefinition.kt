package com.gadarts.shared.data.definitions.characters

import com.badlogic.gdx.math.Vector3
import com.gadarts.shared.assets.definitions.model.ModelDefinition
import com.gadarts.shared.data.type.CharacterType

enum class SimpleCharacterDefinition(
    private val hp: Float,
    private val modelDefinition: ModelDefinition,
    private val gravity: Vector3,
    private val linearFactor: Vector3,
    private val angularFactor: Vector3,
    private val startHeight: Float,
    private val flyer: Boolean,
    private val gibable: Boolean,
    private val corpseModelDefinitions: List<ModelDefinition>,
    private val fuelConsumptionPace: Float,
    private val linearDamping: Float,
    private val angularDamping: Float,
    private val friction: Float,
    private val useSeparateTransformObjectForPhysics: Boolean
) : CharacterDefinition {
    APACHE(
        125F,
        ModelDefinition.APACHE,
        Vector3.Zero,
        Vector3(1F, 0F, 1F),
        Vector3(Vector3.Y),
        CharacterDefinition.FLYER_HEIGHT,
        true,
        true,
        listOf(ModelDefinition.APACHE_DEAD),
        0.05F,
        0.4F,
        0.75F,
        1.5F,
        true
    );

    override fun isOriginPointAtBottom(): Boolean {
        return false
    }

    override fun isFlyer(): Boolean {
        return flyer
    }

    override fun getLinearFactor(output: Vector3): Vector3 {
        return output.set(linearFactor)
    }

    override fun getAngularFactor(output: Vector3): Vector3 {
        return output.set(angularFactor)
    }

    override fun getMovementHeight(): Float {
        return startHeight
    }

    override fun getGravity(output: Vector3): Vector3 {
        return output.set(gravity)
    }

    override fun isUseSeparateTransformObjectForPhysics(): Boolean {
        return useSeparateTransformObjectForPhysics
    }

    override fun getModelDefinition(): ModelDefinition {
        return modelDefinition
    }

    override fun getSmokeEmissionRelativePosition(output: Vector3): Vector3 {
        return Vector3.Zero
    }

    override fun getHP(): Float {
        return hp
    }

    override fun getName(): String {
        return name
    }

    override fun getCharacterType(): CharacterType {
        return CharacterType.SIMPLE
    }

    override fun isRandomizeRotation(): Boolean {
        return false
    }

    override fun getScale(): Float {
        return 1F
    }

    override fun isGibable(): Boolean {
        return gibable
    }

    override fun getCorpseModelDefinitions(): List<ModelDefinition> {
        return corpseModelDefinitions
    }

    override fun isMarksNodeAsBlocked(): Boolean {
        return false
    }

    override fun getFuelConsumptionPace(): Float {
        return fuelConsumptionPace
    }

    override fun isConsumingFuelOnIdle(): Boolean {
        return true
    }

    override fun isPlaceable(): Boolean {
        return false
    }

    override fun getLinearDamping(): Float {
        return linearDamping
    }

    override fun getAngularDamping(): Float {
        return angularDamping
    }

    override fun getFriction(): Float {
        return friction
    }

    override fun getMass(): Float {
        return 10F
    }

    override fun isDeployable(): Boolean {
        return true
    }

    override fun definitionPerColor(): Boolean {
        return true
    }
}
