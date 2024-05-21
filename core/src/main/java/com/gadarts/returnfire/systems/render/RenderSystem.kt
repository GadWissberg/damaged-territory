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
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.Services
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.GroundComponent
import com.gadarts.returnfire.components.IndependentDecalComponent
import com.gadarts.returnfire.components.ModelInstanceComponent
import com.gadarts.returnfire.components.arm.PrimaryArmComponent
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.GameSessionData
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.SystemEvents
import kotlin.math.max

class RenderSystem : GameEntitySystem(), Disposable {

    private lateinit var decalBatch: DecalBatch
    private lateinit var modelBatch: ModelBatch
    private lateinit var renderSystemRelatedEntities: RenderSystemRelatedEntities
    private var axisModelHandler = AxisModelHandler()
    private lateinit var shadowLight: DirectionalShadowLight
    private lateinit var environment: Environment
    private lateinit var shadowBatch: ModelBatch

    override fun initialize(gameSessionData: GameSessionData, services: Services) {
        super.initialize(gameSessionData, services)
        initializeDirectionalLightAndShadows()
        renderSystemRelatedEntities = RenderSystemRelatedEntities(
            engine!!.getEntitiesFor(
                Family.all(ModelInstanceComponent::class.java)
                    .exclude(GroundComponent::class.java)
                    .get()
            ),
            engine.getEntitiesFor(Family.all(PrimaryArmComponent::class.java).get()),
            engine.getEntitiesFor(Family.all(ChildDecalComponent::class.java).get()),
            engine.getEntitiesFor(Family.all(IndependentDecalComponent::class.java).get())
        )
        decalBatch = DecalBatch(DECALS_POOL_SIZE, CameraGroupStrategy(gameSessionData.camera))
        modelBatch = ModelBatch()
    }

    private fun initializeDirectionalLightAndShadows() {
        environment = Environment()
        environment.set(
            ColorAttribute(
                ColorAttribute.AmbientLight,
                Color(0.9F, 0.9F, 0.9F, 1F)
            )
        )
        shadowLight = DirectionalShadowLight(
            2056,
            2056,
            60f,
            60f,
            .1f,
            150f
        )
        val dirValue = 0.4f
        shadowLight.set(dirValue, dirValue, dirValue, 40.0f, -35f, -35f)
        environment.add(shadowLight)
        environment.shadowMap = shadowLight
        shadowBatch = ModelBatch(DepthShaderProvider())
    }

    override fun update(deltaTime: Float) {
        shadowLight.begin(Vector3.Zero, gameSessionData.camera.direction)
        renderModels(
            shadowBatch, shadowLight.camera,
            renderModelCache = true,
            applyEnvironment = false
        )
        shadowLight.end()
        resetDisplay()
        renderModels(modelBatch, gameSessionData.camera, true, true)
        renderDecals(deltaTime)
    }

    override fun resume(delta: Long) {

    }

    override fun dispose() {
        modelBatch.dispose()
        shadowBatch.dispose()
        shadowLight.dispose()
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

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
        renderSparks()
        for (entity in renderSystemRelatedEntities.childEntities) {
            renderChildren(entity, deltaTime)
        }
        renderIndependentDecals()
        decalBatch.flush()
        Gdx.gl.glDepthMask(true)
    }

    private fun renderIndependentDecals() {
        for (entity in renderSystemRelatedEntities.decalEntities) {
            val decal = ComponentsMapper.independentDecal.get(entity).decal
            faceDecalToCamera(decal)
            decalBatch.add(decal)
        }
    }

    private fun renderSparks() {
        for (entity in renderSystemRelatedEntities.armEntities) {
            renderSpark(ComponentsMapper.primaryArm.get(entity))
            renderSpark(ComponentsMapper.secondaryArm.get(entity))
        }
    }

    private fun isVisible(entity: Entity): Boolean {
        val modelInsComp = ComponentsMapper.modelInstance[entity]
        if (modelInsComp.hidden) return false

        val pos: Vector3 = modelInsComp.modelInstance.transform.getTranslation(auxVector3_1)
        val center: Vector3 = pos.add(modelInsComp.getBoundingBox(auxBox).getCenter(auxVector3_2))
        val dims: Vector3 = auxBox.getDimensions(auxVector3_2)
        dims.x = max(dims.x, max(dims.y, dims.z))
        dims.y = max(dims.x, max(dims.y, dims.z))
        dims.z = max(dims.x, max(dims.y, dims.z))
        return gameSessionData.camera.frustum.boundsInFrustum(center, dims)
    }

    private fun renderModels(
        batch: ModelBatch,
        camera: Camera,
        renderModelCache: Boolean,
        applyEnvironment: Boolean
    ) {
        batch.begin(camera)
        axisModelHandler.render(batch)
        for (entity in renderSystemRelatedEntities.modelInstanceEntities) {
            renderModel(entity, batch, applyEnvironment)
        }
        if (renderModelCache) {
            if (applyEnvironment) {
                batch.render(gameSessionData.modelCache, environment)
            } else {
                batch.render(gameSessionData.modelCache)
            }
        }
        batch.end()
    }

    private fun renderSpark(armComp: ArmComponent) {
        if (TimeUtils.timeSinceMillis(armComp.displaySpark) <= SPARK_DURATION) {
            val sparkFrames = armComp.armProperties.sparkFrames
            val frame = sparkFrames[MathUtils.random(sparkFrames.size - 1)]
            if (armComp.sparkDecal.textureRegion != frame) {
                armComp.sparkDecal.textureRegion = frame
            }
            faceDecalToCamera(armComp.sparkDecal)
            decalBatch.add(armComp.sparkDecal)
        }
    }

    private fun faceDecalToCamera(decal: Decal) {
        val camera = gameSessionData.camera
        decal.lookAt(auxVector3_1.set(decal.position).sub(camera.direction), camera.up)
    }

    private fun renderModel(entity: Entity, batch: ModelBatch, applyEnvironment: Boolean) {
        if (isVisible(entity)) {
            val modelInstanceComponent = ComponentsMapper.modelInstance.get(entity)
            val modelInstance = modelInstanceComponent.modelInstance
            if (applyEnvironment) {
                batch.render(modelInstance, environment)
            } else {
                batch.render(modelInstance)
            }
        }
    }

    private fun renderChildren(
        entity: Entity,
        deltaTime: Float,
    ) {
        val childComponent = ComponentsMapper.childDecal.get(entity)
        val children = childComponent.decals
        val modelInstance = ComponentsMapper.modelInstance.get(entity).modelInstance
        val parentPosition = modelInstance.transform.getTranslation(auxVector3_1)
        val parentRotation = modelInstance.transform.getRotation(auxQuat)
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
        decalBatch.add(child.decal)
    }

    companion object {
        val auxVector3_1 = Vector3()
        val auxVector3_2 = Vector3()
        val auxQuat = Quaternion()
        val auxBox = BoundingBox()
        const val ROT_STEP = 1600F
        const val SPARK_DURATION = 40L
        const val DECALS_POOL_SIZE = 200
    }

}
