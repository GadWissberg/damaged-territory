package com.gadarts.returnfire.systems.render

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.systems.data.GameSessionDataRender
import com.gadarts.returnfire.systems.render.RenderSystem.Companion.auxBox
import com.gadarts.returnfire.systems.render.RenderSystem.Companion.auxVector3_1
import com.gadarts.returnfire.systems.render.RenderSystem.Companion.auxVector3_2
import com.gadarts.returnfire.systems.render.RenderSystem.Companion.auxVector3_3

class ModelsRenderer(
    private val relatedEntities: RenderSystemRelatedEntities,
    private val renderFlags: RenderFlags,
    private val renderData: GameSessionDataRender,
    private val batches: RenderSystemBatches
) : Disposable {
    private var axisModelHandler = AxisModelHandler()
    private val shadowLight: DirectionalShadowLight by lazy {
        DirectionalShadowLight(
            2056,
            2056,
            30f,
            30f,
            .1f,
            150f
        )
    }

    fun renderModels(
        batch: ModelBatch,
        camera: Camera,
        applyEnvironment: Boolean,
        renderParticleEffects: Boolean,
        forShadow: Boolean
    ) {
        batch.begin(camera)
        axisModelHandler.render(batch)
        for (entity in relatedEntities.modelInstanceEntities) {
            renderModel(entity, batch, applyEnvironment, forShadow)
        }
        if (!GameDebugSettings.HIDE_FLOOR && renderFlags.renderGround) {
            if (applyEnvironment) {
                batch.render(renderData.modelCache, environment)
            } else {
                batch.render(renderData.modelCache)
            }
        }
        if (renderParticleEffects) {
            batches.modelBatch.render(renderData.particleSystem, environment)
        }
        batch.end()
    }

    private fun isConsideredCharacter(entity: Entity) =
        (ComponentsMapper.character.has(entity) || ComponentsMapper.turret.has(entity))

    private fun renderModel(entity: Entity, batch: ModelBatch, applyEnvironment: Boolean, forShadow: Boolean = false) {
        if (isVisible(entity, forShadow)) {
            val modelInstanceComponent = ComponentsMapper.modelInstance.get(entity)
            val gameModelInstance = modelInstanceComponent.gameModelInstance
            val isShadow = forShadow && gameModelInstance.shadow != null
            val modelInstance = if (isShadow) gameModelInstance.shadow else gameModelInstance.modelInstance
            if (applyEnvironment) {
                batch.render(modelInstance, environment)
            } else {
                batch.render(modelInstance)
            }
            if (!modelInstanceComponent.hidden
                && modelInstanceComponent.hideAt != -1L
                && modelInstanceComponent.hideAt <= TimeUtils.millis()
            ) {
                modelInstanceComponent.hidden = true
                modelInstanceComponent.hideAt = -1L
            }
        }
    }

    fun renderWaterWaves() {
        batches.modelBatch.begin(renderData.camera)
        Gdx.gl.glDepthMask(false)
        for (entity in relatedEntities.waterWaveEntities) {
            renderModel(entity, batches.modelBatch, true)
        }
        batches.modelBatch.end()
        Gdx.gl.glDepthMask(true)
    }

    private fun isVisible(entity: Entity, extendBoundingBoxSize: Boolean): Boolean {
        val modelInsComp = ComponentsMapper.modelInstance[entity]
        val gameModelInstance = modelInsComp.gameModelInstance
        val boundingBox = gameModelInstance.getBoundingBox(auxBox)
        val dims: Vector3 = boundingBox.getDimensions(auxVector3_2)

        if (modelInsComp.hidden) return false
        if (dims.isZero) return false
        if (!renderFlags.renderCharacters && (isConsideredCharacter(entity))) return false
        if (ComponentsMapper.player.has(entity)) return true
        if (!renderFlags.renderAmbient && ComponentsMapper.amb.has(entity)) return false

        val center: Vector3 =
            gameModelInstance.modelInstance.transform.getTranslation(auxVector3_1)
        if (extendBoundingBoxSize) {
            dims.scl(1F)
        }
        val frustum = renderData.camera.frustum
        return if (gameModelInstance.sphere) frustum.sphereInFrustum(
            gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_3
            ), dims.len2() / 2F
        )
        else frustum.boundsInFrustum(center, dims)
    }

    fun renderShadows() {
        if (renderFlags.renderShadows) {
            val camera = renderData.camera
            shadowLight.begin(
                auxVector3_1.set(camera.position).add(-2F, 0F, -4F),
                camera.direction
            )
            renderModels(
                batch = batches.shadowBatch,
                camera = shadowLight.camera,
                applyEnvironment = false,
                renderParticleEffects = false,
                forShadow = true
            )
            shadowLight.end()
        }
    }

    private val environment: Environment by lazy { Environment() }

    fun disableShadow() {
        environment.remove(shadowLight)
        environment.shadowMap = null
    }

    fun initializeDirectionalLightAndShadows() {
        environment.set(
            ColorAttribute(
                ColorAttribute.AmbientLight,
                Color(0.9F, 0.9F, 0.9F, 1F)
            )
        )
        val dirValue = 0.4f
        shadowLight.set(dirValue, dirValue, dirValue, 0.4F, -0.6f, -0.35f)
        enableShadow()
    }

    override fun dispose() {
        shadowLight.dispose()
    }

    fun enableShadow() {
        environment.add(shadowLight)
        environment.shadowMap = shadowLight
    }

}
