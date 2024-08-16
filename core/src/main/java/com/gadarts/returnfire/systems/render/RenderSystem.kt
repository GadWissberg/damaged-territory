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
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ArmComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.GroundComponent
import com.gadarts.returnfire.components.IndependentDecalComponent
import com.gadarts.returnfire.components.arm.PrimaryArmComponent
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.components.model.ModelInstanceComponent
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.CollisionShapesDebugDrawing
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

class RenderSystem : GameEntitySystem(), Disposable {

    private lateinit var renderSystemRelatedEntities: RenderSystemRelatedEntities
    private var axisModelHandler = AxisModelHandler()
    private lateinit var shadowLight: DirectionalShadowLight
    private lateinit var environment: Environment
    private lateinit var renderSystemBatches: RenderSystemBatches

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = emptyMap()

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
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
        renderSystemBatches =
            RenderSystemBatches(
                DecalBatch(DECALS_POOL_SIZE, CameraGroupStrategy(gameSessionData.camera)),
                ModelBatch(),
                ModelBatch(DepthShaderProvider())
            )
    }

    override fun update(deltaTime: Float) {
        shadowLight.begin(
            auxVector3_1.set(gameSessionData.camera.position).add(-2F, 0F, -4F),
            gameSessionData.camera.direction
        )
        renderModels(
            renderSystemBatches.shadowBatch,
            shadowLight.camera,
            applyEnvironment = false,
            renderParticleEffects = false
        )
        shadowLight.end()
        resetDisplay()
        renderModels(
            renderSystemBatches.modelBatch,
            gameSessionData.camera,
            applyEnvironment = true,
            renderParticleEffects = true
        )
        renderCollisionShapes()
        renderDecals(deltaTime)
    }

    private fun renderCollisionShapes() {
        if (!GameDebugSettings.SHOW_COLLISION_SHAPES) return

        val debugDrawingMethod: CollisionShapesDebugDrawing? = gameSessionData.gameSessionPhysicsData.debugDrawingMethod
        debugDrawingMethod?.drawCollisionShapes(gameSessionData.camera)
    }

    override fun resume(delta: Long) {

    }

    override fun dispose() {
        renderSystemBatches.dispose()
        shadowLight.dispose()
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
            30f,
            30f,
            .1f,
            150f
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
        renderSparks()
        for (entity in renderSystemRelatedEntities.childEntities) {
            renderChildren(entity, deltaTime)
        }
        renderIndependentDecals()
        renderSystemBatches.decalBatch.flush()
        Gdx.gl.glDepthMask(true)
    }

    private fun renderIndependentDecals() {
        for (entity in renderSystemRelatedEntities.decalEntities) {
            val independentDecalsToRemove = renderSystemRelatedEntities.independentDecalsToRemove
            independentDecalsToRemove.clear()
            val independentDecalComponent = ComponentsMapper.independentDecal.get(entity)
            if (independentDecalComponent.ttl <= TimeUtils.millis()) {
                independentDecalsToRemove.add(entity)
            } else {
                val decal = independentDecalComponent.decal
                faceDecalToCamera(decal)
                renderSystemBatches.decalBatch.add(decal)
            }
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
        if (ComponentsMapper.player.has(entity)) return true

        val center: Vector3 =
            modelInsComp.gameModelInstance.getBoundingBox(auxBox).getCenter(auxVector3_1)
        val dims: Vector3 = auxBox.getDimensions(auxVector3_2).scl(4.7F)
        return if (modelInsComp.gameModelInstance.isSphere) gameSessionData.camera.frustum.sphereInFrustum(
            modelInsComp.gameModelInstance.modelInstance.transform.getTranslation(
                auxVector3_3
            ), dims.len2() / 2F
        )
        else gameSessionData.camera.frustum.boundsInFrustum(center, dims)
    }

    private fun renderModels(
        batch: ModelBatch,
        camera: Camera,
        applyEnvironment: Boolean,
        renderParticleEffects: Boolean
    ) {
        batch.begin(camera)
        axisModelHandler.render(batch)
        for (entity in renderSystemRelatedEntities.modelInstanceEntities) {
            renderModel(entity, batch, applyEnvironment)
        }
        if (!GameDebugSettings.HIDE_FLOOR) {
            if (applyEnvironment) {
                batch.render(gameSessionData.gameSessionDataRender.modelCache, environment)
            } else {
                batch.render(gameSessionData.gameSessionDataRender.modelCache)
            }
        }
        if (renderParticleEffects) {
            renderSystemBatches.modelBatch.render(gameSessionData.gameSessionDataRender.particleSystem, environment)
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
            renderSystemBatches.decalBatch.add(armComp.sparkDecal)
        }
    }

    private fun faceDecalToCamera(decal: Decal) {
        val camera = gameSessionData.camera
        decal.lookAt(auxVector3_1.set(decal.position).sub(camera.direction), camera.up)
    }

    private fun renderModel(entity: Entity, batch: ModelBatch, applyEnvironment: Boolean) {
        if (isVisible(entity)) {
            val modelInstanceComponent = ComponentsMapper.modelInstance.get(entity)
            val modelInstance = modelInstanceComponent.gameModelInstance
            if (applyEnvironment) {
                batch.render(modelInstance.modelInstance, environment)
            } else {
                batch.render(modelInstance.modelInstance)
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
        renderSystemBatches.decalBatch.add(child.decal)
    }

    companion object {
        val auxVector3_1 = Vector3()
        val auxVector3_2 = Vector3()
        val auxVector3_3 = Vector3()
        val auxQuat = Quaternion()
        val auxBox = BoundingBox()
        const val ROT_STEP = 1600F
        const val SPARK_DURATION = 40L
        const val DECALS_POOL_SIZE = 200
    }

}
