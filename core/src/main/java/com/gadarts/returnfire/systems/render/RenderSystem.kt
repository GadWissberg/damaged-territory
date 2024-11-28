package com.gadarts.returnfire.systems.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.decals.CameraGroupStrategy
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.graphics.g3d.decals.DecalBatch
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.TimeUtils
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.GeneralUtils
import com.gadarts.returnfire.Managers
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.GroundBlastComponent
import com.gadarts.returnfire.components.GroundComponent
import com.gadarts.returnfire.components.IndependentDecalComponent
import com.gadarts.returnfire.components.cd.ChildDecal
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.components.model.ModelInstanceComponent
import com.gadarts.returnfire.console.CommandList
import com.gadarts.returnfire.console.commands.ExecutedCommand
import com.gadarts.returnfire.console.commands.SkipDrawingCommand
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.CollisionShapesDebugDrawing
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents

/**
 * Responsible for rendering the 3D models, their shadows, decals and collision shapes.
 */
class RenderSystem : GameEntitySystem(), Disposable {

    private val renderFlags = RenderFlags()
    private lateinit var relatedEntities: RenderSystemRelatedEntities
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
    private val modelsRenderer by lazy {
        ModelsRenderer(
            relatedEntities,
            renderFlags,
            gameSessionData.renderData,
            batches,
        )
    }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.CONSOLE_COMMAND_EXECUTED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, managers: Managers) {
                val command = msg.extraInfo as ExecutedCommand
                if (command.command == CommandList.SKIP_DRAWING) {
                    val parameters = command.parameters
                    parameters.forEach { (parameter, value) ->
                        val alias: String = parameter.lowercase()
                        val renderEnabled = value == "0"
                        when (alias) {
                            SkipDrawingCommand.ShadowsParameter.alias -> {
                                renderFlags.renderShadows = renderEnabled
                                if (renderEnabled) {
                                    modelsRenderer.enableShadow()
                                } else {
                                    modelsRenderer.disableShadow()
                                }
                            }

                            SkipDrawingCommand.GroundParameter.alias -> renderFlags.renderGround = renderEnabled
                            SkipDrawingCommand.CharactersParameter.alias -> renderFlags.renderCharacters = renderEnabled
                            SkipDrawingCommand.EnvironmentParameter.alias -> renderFlags.renderAmbient = renderEnabled
                        }
                    }

                }
            }

        }
    )

    override fun initialize(gameSessionData: GameSessionData, managers: Managers) {
        super.initialize(gameSessionData, managers)
        relatedEntities = RenderSystemRelatedEntities(
            engine!!.getEntitiesFor(
                Family.all(ModelInstanceComponent::class.java)
                    .exclude(GroundComponent::class.java, GroundBlastComponent::class.java)
                    .get()
            ),
            engine.getEntitiesFor(Family.all(ChildDecalComponent::class.java).get()),
            engine.getEntitiesFor(Family.all(IndependentDecalComponent::class.java).get()),
            engine.getEntitiesFor(Family.all(GroundBlastComponent::class.java).get())
        )
        modelsRenderer.initializeDirectionalLightAndShadows()
    }

    override fun update(deltaTime: Float) {
        if (gameSessionData.sessionFinished) return

        modelsRenderer.renderShadows()
        GeneralUtils.clearScreen()
        modelsRenderer.renderModels(
            batch = batches.modelBatch,
            camera = gameSessionData.renderData.camera,
            applyEnvironment = true,
            renderParticleEffects = true,
            forShadow = false
        )
        modelsRenderer.renderWaterWaves()
        renderCollisionShapes()
        renderDecals()
    }

    private fun renderCollisionShapes() {
        if (!GameDebugSettings.SHOW_COLLISION_SHAPES) return

        val debugDrawingMethod: CollisionShapesDebugDrawing? = gameSessionData.gameSessionDataPhysics.debugDrawingMethod
        debugDrawingMethod?.drawCollisionShapes(gameSessionData.renderData.camera)
    }

    override fun resume(delta: Long) {

    }

    override fun dispose() {
        batches.dispose()
        modelsRenderer.dispose()
    }


    private fun renderDecals() {
        Gdx.gl.glDepthMask(false)
        batches.decalBatch
        for (entity in relatedEntities.childEntities) {
            if (renderFlags.renderCharacters || !ComponentsMapper.childDecal.has(entity)) {
                renderChildren(entity)
            }
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


    private fun faceDecalToCamera(decal: Decal) {
        val camera = gameSessionData.renderData.camera
        decal.lookAt(auxVector3_1.set(decal.position).sub(camera.direction), camera.up)
    }


    private fun renderChildren(
        entity: Entity,
    ) {
        val childComponent = ComponentsMapper.childDecal.get(entity)
        if (!childComponent.visible) return

        val children = childComponent.decals
        val modelInstance = ComponentsMapper.modelInstance.get(entity).gameModelInstance
        val parentPosition = modelInstance.modelInstance.transform.getTranslation(auxVector3_1)
        for (child in children) {
            renderChild(child, parentPosition)
        }
    }

    private fun renderChild(
        child: ChildDecal,
        parentPosition: Vector3?
    ) {
        child.decal.position = parentPosition
        child.decal.position.add(child.relativePosition)
        if (child.localRotation != null) {
            child.decal.rotation = child.localRotation
        }
        batches.decalBatch.add(child.decal)
    }

    companion object {
        val auxVector3_1 = Vector3()
        val auxVector3_2 = Vector3()
        val auxVector3_3 = Vector3()
        val auxBox = BoundingBox()
        const val DECALS_POOL_SIZE = 200
    }

}
