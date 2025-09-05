package com.gadarts.returnfire.ecs.systems.render.renderers

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.components.model.GameModelInstance
import com.gadarts.returnfire.ecs.components.model.ModelInstanceComponent
import com.gadarts.returnfire.ecs.systems.data.GameSessionDataRender
import com.gadarts.returnfire.ecs.systems.render.*
import com.gadarts.returnfire.utils.ModelUtils

class ModelsRenderer(
    private val relatedEntities: RenderSystemRelatedEntities,
    private val renderFlags: RenderFlags,
    private val renderData: GameSessionDataRender,
    private val batches: RenderSystemBatches,
    private val modelCaches: List<ModelCache>
) : Disposable {
    private val haloRenderer = HaloRenderer()
    private var axisModelHandler = AxisModelHandler()
    private val shadowLight: DirectionalShadowLight by lazy {
        DirectionalShadowLight(
            SHADOW_MAP_SIZE,
            SHADOW_MAP_SIZE,
            SHADOW_VIEWPORT_SIZE,
            SHADOW_VIEWPORT_SIZE,
            .1f,
            150f
        )
    }
    private val environment: Environment by lazy { Environment() }

    @Suppress("SimplifyBooleanWithConstants")
    fun renderModels(
        batch: ModelBatch,
        camera: Camera,
        applyEnvironment: Boolean,
        renderParticleEffects: Boolean,
        forShadow: Boolean,
        deltaTime: Float
    ) {
        batch.begin(camera)
        axisModelHandler.render(batch)
        for (entity in relatedEntities.modelInstanceEntities) {
            renderModel(entity, batch, applyEnvironment, deltaTime, forShadow)
        }
        if (!GameDebugSettings.HIDE_FLOOR && renderFlags.renderGround) {
            modelCaches.forEach {
                if (applyEnvironment) {
                    batch.render(it, environment)
                } else {
                    batch.render(it)
                }
            }
        }
        batch.end()
        if (renderParticleEffects) {
            batch.begin(camera)
            batches.modelBatch.render(renderData.particleSystem, environment)
            batch.end()
        }
    }

    fun renderWaterWaves(deltaTime: Float) {
        batches.modelBatch.begin(renderData.camera)
        Gdx.gl.glDepthMask(false)
        for (entity in relatedEntities.waterWaveEntities) {
            renderModel(entity, batches.modelBatch, true, deltaTime)
        }
        batches.modelBatch.end()
        Gdx.gl.glDepthMask(true)
    }

    fun renderShadows(deltaTime: Float) {
        if (renderFlags.renderShadows) {
            val camera = renderData.camera
            shadowLight.begin(
                RenderSystem.auxVector3_1.set(camera.position).add(-2F, 0F, -4F),
                camera.direction
            )
            renderModels(
                batch = batches.shadowBatch,
                camera = shadowLight.camera,
                applyEnvironment = false,
                renderParticleEffects = false,
                forShadow = true,
                deltaTime = deltaTime
            )
            shadowLight.end()
        }
    }

    fun disableShadow() {
        environment.remove(shadowLight)
        environment.shadowMap = null
    }

    fun initializeDirectionalLightAndShadows() {
        extracted()
        val dirValue = 0.4f
        shadowLight.set(dirValue, dirValue, dirValue, 0.3F, -0.7f, -0.3f)
        enableShadow()
    }

    fun enableShadow() {
        environment.add(shadowLight)
        environment.shadowMap = shadowLight
    }

    override fun dispose() {
        shadowLight.dispose()
    }

    private fun isConsideredCharacter(entity: Entity) =
        (ComponentsMapper.character.has(entity) || ComponentsMapper.turret.has(entity))

    private fun renderModel(
        entity: Entity,
        batch: ModelBatch,
        applyEnvironment: Boolean,
        deltaTime: Float,
        forShadow: Boolean = false,
    ) {
        if (isVisible(entity, forShadow)) {
            val modelInstanceComponent = ComponentsMapper.modelInstance.get(entity)
            val gameModelInstance = modelInstanceComponent.gameModelInstance

            renderGameModelInstance(gameModelInstance, forShadow, applyEnvironment, batch)
            if (!modelInstanceComponent.hidden
                && modelInstanceComponent.hideAt != -1L
                && modelInstanceComponent.hideAt <= TimeUtils.millis()
            ) {
                modelInstanceComponent.hidden = true
                modelInstanceComponent.hideAt = -1L
            }
            renderChildModelInstance(entity, forShadow, applyEnvironment, batch)
            renderFrontWheels(entity, forShadow, applyEnvironment, batch)
            val haloEffect = modelInstanceComponent.haloEffect
            if (!forShadow && haloEffect != null && haloEffect.visible) {
                haloRenderer.render(
                    ModelUtils.getPositionOfModel(entity, RenderSystem.auxVector3_1), batch,
                    environment,
                    haloEffect,
                    deltaTime
                )
            }
        }
    }

    private fun renderFrontWheels(entity: Entity, forShadow: Boolean, applyEnvironment: Boolean, batch: ModelBatch) {
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(entity)
        val frontWheelComponent = ComponentsMapper.frontWheelsComponent.get(entity)
        if (frontWheelComponent != null) {
            renderWheel(
                modelInstanceComponent,
                forShadow,
                applyEnvironment,
                batch,
                frontWheelComponent.leftWheel,
                frontWheelComponent.leftRelativeTransform,
                frontWheelComponent.steeringRotation
            )
            renderWheel(
                modelInstanceComponent,
                forShadow,
                applyEnvironment,
                batch,
                frontWheelComponent.rightWheel,
                frontWheelComponent.rightRelativeTransform,
                frontWheelComponent.steeringRotation
            )
        }
    }

    private fun renderWheel(
        modelInstanceComponent: ModelInstanceComponent,
        forShadow: Boolean,
        applyEnvironment: Boolean,
        batch: ModelBatch,
        wheelGameModelInstance: GameModelInstance,
        relativeTransform: Matrix4,
        steeringRotation: Float
    ) {
        val wheelModelInstance = wheelGameModelInstance.modelInstance
        val transform = wheelModelInstance.transform
        transform.set(modelInstanceComponent.gameModelInstance.modelInstance.transform)
        transform.mul(
            relativeTransform
        )
        transform.rotate(Vector3.Y, steeringRotation)
        renderGameModelInstance(
            wheelGameModelInstance,
            forShadow,
            applyEnvironment,
            batch
        )
    }

    private fun renderChildModelInstance(
        entity: Entity,
        forShadow: Boolean,
        applyEnvironment: Boolean,
        batch: ModelBatch
    ) {
        val modelInstanceComponent = ComponentsMapper.modelInstance.get(entity)
        val childModelInstanceComponent = ComponentsMapper.childModelInstance.get(entity)
        if (childModelInstanceComponent != null && childModelInstanceComponent.visible) {
            val childGameModelInstance = childModelInstanceComponent.gameModelInstance
            val childModelInstance = childGameModelInstance.modelInstance
            val modelInstancePosition = modelInstanceComponent.gameModelInstance.modelInstance.transform.getTranslation(
                RenderSystem.auxVector3_1
            )
            childModelInstance.transform.setTranslation(
                modelInstancePosition
            )
            if (childModelInstanceComponent.followParentRotation) {
                childModelInstance.transform.set(
                    modelInstanceComponent.gameModelInstance.modelInstance.transform.getRotation(
                        auxQuat1
                    )
                ).trn(modelInstancePosition)
            }
            childModelInstance.transform.translate(RenderSystem.auxVector3_2.set(childModelInstanceComponent.relativePosition))
            renderGameModelInstance(
                childGameModelInstance,
                forShadow,
                applyEnvironment,
                batch
            )
        }
    }

    private fun renderGameModelInstance(
        gameModelInstance: GameModelInstance,
        forShadow: Boolean,
        applyEnvironment: Boolean,
        batch: ModelBatch
    ) {
        val isShadow = forShadow && gameModelInstance.shadow != null
        val modelInstance = if (isShadow) gameModelInstance.shadow else gameModelInstance.modelInstance
        if (applyEnvironment) {
            batch.render(modelInstance, environment)
        } else {
            batch.render(modelInstance)
        }
    }

    private fun isVisible(entity: Entity, extendBoundingBoxSize: Boolean): Boolean {
        val modelInsComp = ComponentsMapper.modelInstance[entity]
        val gameModelInstance = modelInsComp.gameModelInstance
        val boundingBox = gameModelInstance.getBoundingBox(RenderSystem.auxBox)
        val dims: Vector3 = boundingBox.getDimensions(RenderSystem.auxVector3_2)
        if (modelInsComp.hidden) return false
        if (dims.isZero) return false
        if (!renderFlags.renderCharacters && (isConsideredCharacter(entity))) return false
        if (ComponentsMapper.player.has(entity)) return true
        if (!renderFlags.renderAmbient && ComponentsMapper.amb.has(entity)) return false

        val center: Vector3 =
            gameModelInstance.modelInstance.transform.getTranslation(RenderSystem.auxVector3_1)
        dims.scl(if (extendBoundingBoxSize) 16.6F else 1.3F)

        val frustum = renderData.camera.frustum
        val isInFrustum = if (gameModelInstance.sphere) frustum.sphereInFrustum(
            gameModelInstance.modelInstance.transform.getTranslation(
                auxVector
            ), dims.len2()
        )
        else frustum.boundsInFrustum(center, dims)

        return isInFrustum
    }

    private fun extracted() {
        environment.set(
            ColorAttribute(
                ColorAttribute.AmbientLight,
                Color(0.9F, 0.9F, 0.9F, 1F)
            )
        )
    }

    companion object {
        private const val SHADOW_VIEWPORT_SIZE: Float = 38F
        private const val SHADOW_MAP_SIZE = 2056
        private val auxVector = Vector3()
        private val auxQuat1 = Quaternion()
    }
}