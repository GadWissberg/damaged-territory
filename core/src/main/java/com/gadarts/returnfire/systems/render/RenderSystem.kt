package com.gadarts.returnfire.systems.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.GroundBlastComponent
import com.gadarts.returnfire.components.GroundComponent
import com.gadarts.returnfire.components.IndependentDecalComponent
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.components.model.ModelInstanceComponent
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.CollisionShapesDebugDrawing
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class RenderSystem : GameEntitySystem(), Disposable {

    private val relatedEntities: RenderSystemRelatedEntities by lazy {
        RenderSystemRelatedEntities(
            engine!!.getEntitiesFor(
                Family.all(ModelInstanceComponent::class.java)
                    .exclude(GroundComponent::class.java, GroundBlastComponent::class.java)
                    .get()
            ),
            engine.getEntitiesFor(Family.all(ChildDecalComponent::class.java).get()),
            engine.getEntitiesFor(Family.all(IndependentDecalComponent::class.java).get()),
            engine.getEntitiesFor(Family.all(GroundBlastComponent::class.java).get())
        )
    }
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
    private val environment: Environment by lazy { Environment() }
    private val batches: RenderSystemBatches by lazy {
        RenderSystemBatches(
            DecalBatch(
                DECALS_POOL_SIZE,
                CameraGroupStrategy(gameSessionData.renderData.camera)
            ),
            ModelBatch(),
            ModelBatch(DepthShaderProvider())
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        initializeDirectionalLightAndShadows()
    }

    override fun update(deltaTime: Float) {
        if (gameSessionData.sessionFinished) return

        val camera = gameSessionData.renderData.camera
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
        resetDisplay()
        renderModels(
            batch = batches.modelBatch,
            camera = camera,
            applyEnvironment = true,
            renderParticleEffects = true,
            forShadow = false
        )
        batches.modelBatch.begin(camera)
        Gdx.gl.glDepthMask(false)
        for (entity in relatedEntities.waterWaveEntities) {
            renderModel(entity, batches.modelBatch, true)
        }
        batches.modelBatch.end()
        Gdx.gl.glDepthMask(true)
        renderCollisionShapes()
        renderDecals(deltaTime)
    }

    @Suppress("KotlinConstantConditions")
    private fun renderCollisionShapes() {
        if (!GameDebugSettings.SHOW_COLLISION_SHAPES) return

        val debugDrawingMethod: CollisionShapesDebugDrawing? = gameSessionData.gameSessionDataPhysics.debugDrawingMethod
        debugDrawingMethod?.drawCollisionShapes(gameSessionData.renderData.camera)
    }

    override fun resume(delta: Long) {

    }

    override fun dispose() {
        batches.dispose()
        shadowLight.dispose()
    }

    private fun initializeDirectionalLightAndShadows() {
        environment.set(
            ColorAttribute(
                ColorAttribute.AmbientLight,
                Color(0.9F, 0.9F, 0.9F, 1F)
            )
        )
        val dirValue = 0.4f
        shadowLight.set(dirValue, dirValue, dirValue, 0.4F, -0.6f, -0.35f)
        environment.add(shadowLight)
        environment.shadowMap = shadowLight
    }

    private fun resetDisplay() {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        ScreenUtils.clear(Color.BLACK, true)
        Gdx.gl.glClearColor(0F, 0F, 0F, 1F)
        Gdx.gl.glClear(
            GL20.GL_COLOR_BUFFER_BIT
                    or GL20.GL_DEPTH_BUFFER_BIT
                    or if (Gdx.graphics.bufferFormat.coverageSampling) GL20.GL_COVERAGE_BUFFER_BIT_NV else 0
        )
    }

    private fun renderDecals(deltaTime: Float) {
        Gdx.gl.glDepthMask(false)
        for (entity in relatedEntities.childEntities) {
            renderChildren(entity, deltaTime)
        }
        renderIndependentDecals()
        batches.decalBatch.flush()
        Gdx.gl.glDepthMask(true)
    }

    private fun renderIndependentDecals() {
        for (entity in relatedEntities.decalEntities) {
            val independentDecalsToRemove = relatedEntities.independentDecalsToRemove
            independentDecalsToRemove.clear()
            val independentDecalComponent = ComponentsMapper.independentDecal.get(entity)
            if (independentDecalComponent.ttl <= TimeUtils.millis()) {
                independentDecalsToRemove.add(entity)
            } else {
                val decal = independentDecalComponent.decal
                faceDecalToCamera(decal)
                batches.decalBatch.add(decal)
            }
        }

    }

    private fun isVisible(entity: Entity, extendBoundingBoxSize: Boolean): Boolean {
        val modelInsComp = ComponentsMapper.modelInstance[entity]
        val gameModelInstance = modelInsComp.gameModelInstance
        val boundingBox = gameModelInstance.getBoundingBox(auxBox)
        val dims: Vector3 = boundingBox.getDimensions(auxVector3_2)

        if (modelInsComp.hidden) return false
        if (ComponentsMapper.player.has(entity)) return true
        if (dims.isZero) return false

        val center: Vector3 =
            gameModelInstance.modelInstance.transform.getTranslation(auxVector3_1)
        if (extendBoundingBoxSize) {
            dims.scl(6F)
        }
        val frustum = gameSessionData.renderData.camera.frustum
        return if (gameModelInstance.sphere) frustum.sphereInFrustum(
            gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_3
            ), dims.len2() / 2F
        )
        else frustum.boundsInFrustum(center, dims)
    }

    @Suppress("KotlinConstantConditions")
    private fun renderModels(
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
        if (!GameDebugSettings.HIDE_FLOOR) {
            if (applyEnvironment) {
                batch.render(gameSessionData.renderData.modelCache, environment)
            } else {
                batch.render(gameSessionData.renderData.modelCache)
            }
        }
        if (renderParticleEffects) {
            batches.modelBatch.render(gameSessionData.renderData.particleSystem, environment)
        }
        batch.end()
    }

    private fun faceDecalToCamera(decal: Decal) {
        val camera = gameSessionData.renderData.camera
        decal.lookAt(auxVector3_1.set(decal.position).sub(camera.direction), camera.up)
    }

    private fun renderModel(entity: Entity, batch: ModelBatch, applyEnvironment: Boolean, forShadow: Boolean = false) {
        if (isVisible(entity, forShadow)) {
            val modelInstanceComponent = ComponentsMapper.modelInstance.get(entity)
            val modelInstance = modelInstanceComponent.gameModelInstance
            if (applyEnvironment) {
                batch.render(modelInstance.modelInstance, environment)
            } else {
                batch.render(modelInstance.modelInstance)
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

    private fun renderChildren(
        entity: Entity,
        deltaTime: Float,
    ) {
        val childComponent = ComponentsMapper.childDecal.get(entity)
        val children = childComponent.decals
        val modelInstance = ComponentsMapper.modelInstance.get(entity).gameModelInstance
        val parentPosition = modelInstance.modelInstance.transform.getTranslation(auxVector3_1)
        val parentRotation = modelInstance.modelInstance.transform.getRotation(auxQuat)
        for (child in children) {
            renderChild(child, parentRotation, deltaTime, parentPosition)
        }
    }

    private fun renderChild(
        child: ChildDecal,
        parentRotation: Quaternion?,
        deltaTime: Float,
        parentPosition: Vector3?
    ) {
        child.decal.rotation = parentRotation
        child.decal.rotateX(90F)
        child.decal.rotateZ(child.rotationStep.angleDeg())
        child.rotationStep.setAngleDeg(child.rotationStep.angleDeg() + ROT_STEP * deltaTime)
        child.decal.position = parentPosition
        batches.decalBatch.add(child.decal)
    }

    companion object {
        val auxVector3_1 = Vector3()
        val auxVector3_2 = Vector3()
        val auxVector3_3 = Vector3()
        val auxQuat = Quaternion()
        val auxBox = BoundingBox()
        const val ROT_STEP = 1600F
        const val DECALS_POOL_SIZE = 200
    }

}
