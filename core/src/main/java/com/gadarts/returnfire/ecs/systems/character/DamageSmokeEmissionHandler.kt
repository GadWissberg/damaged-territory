package com.gadarts.returnfire.ecs.systems.character

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.model.GameModelInstance

class DamageSmokeEmissionHandler {
    fun update(character: Entity) {
        val characterComponent = ComponentsMapper.character.get(character)
        if (characterComponent.applyDamageEmission) {
            applyDamageEmissionOnCharacter(character, EMISSION)
            characterComponent.applyDamageEmission = false
        } else {
            val lastDamageEmission = characterComponent.lastDamageEmission
            if (lastDamageEmission != null) {
                val elapsed = TimeUtils.timeSinceMillis(lastDamageEmission)
                val duration = 250L
                if (elapsed > duration) {
                    applyDamageEmissionOnCharacter(character, 0f)
                    val modelInstanceComponent = ComponentsMapper.modelInstance.get(character)
                    val emission = modelInstanceComponent.gameModelInstance.modelInstance
                        .materials[0]
                        .get(ColorAttribute.Emissive) as ColorAttribute
                    emission.color.set(Color.BLACK)
                    characterComponent.resetLastDamageEmission()
                } else {
                    // Interpolate from 0.75 -> 0 over 250ms
                    val t = elapsed.toFloat() / duration.toFloat()
                    val fadeValue = MathUtils.lerp(EMISSION, 0f, t)
                    applyDamageEmissionOnCharacter(character, fadeValue)
                }
            }
        }
    }

    private fun applyDamageEmissionOnCharacter(
        character: Entity, value: Float
    ) {
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(character)
        applyDamageEmissionOnModel(modelInstanceComponent.gameModelInstance, value)

        val childModelInstanceComponent = ComponentsMapper.childModelInstance.get(character)
        if (childModelInstanceComponent != null) {
            applyDamageEmissionOnModel(childModelInstanceComponent.gameModelInstance, value)
        }
        if (ComponentsMapper.turretBase.has(character)) {
            val turretBaseComponent = ComponentsMapper.turretBase.get(character)
            val turret = turretBaseComponent.turret
            val turretModelInstanceComponent = ComponentsMapper.modelInstance.get(turret)
            applyDamageEmissionOnModel(turretModelInstanceComponent.gameModelInstance, value)
            val cannon = ComponentsMapper.turret.get(turret).cannon
            if (cannon != null) {
                val cannonModelInstanceComponent = ComponentsMapper.modelInstance.get(cannon)
                applyDamageEmissionOnModel(cannonModelInstanceComponent.gameModelInstance, value)
            }
        }
    }

    private fun applyDamageEmissionOnModel(
        gameModelInstance: GameModelInstance,
        value: Float
    ) {
        val mainMaterialIndex =
            gameModelInstance.gameModelInstanceInfo?.modelDefinition?.mainMaterialIndex
        val emission =
            gameModelInstance.modelInstance.materials[mainMaterialIndex
                ?: 0].get(
                ColorAttribute.Emissive
            ) as ColorAttribute
        emission.color.set(
            value, value, value, 1F
        )
    }

    companion object {
        private const val EMISSION = 0.75f
    }
}
