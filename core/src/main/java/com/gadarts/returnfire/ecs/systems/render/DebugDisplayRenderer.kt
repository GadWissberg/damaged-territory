package com.gadarts.returnfire.ecs.systems.render

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.gadarts.returnfire.ecs.components.ComponentsMapper
import com.gadarts.returnfire.ecs.systems.data.CollisionShapesDebugDrawing
import com.gadarts.returnfire.ecs.systems.render.RenderSystem.Companion.auxMatrix
import com.gadarts.shared.GameAssetManager

class DebugDisplayRenderer(
    private val assetsManager: GameAssetManager,
    private val shapeRenderer: ShapeRenderer,
    private val groundAiCharacterEntities: ImmutableArray<Entity>,
    private val camera: PerspectiveCamera,
    private val debugDrawingMethod: CollisionShapesDebugDrawing?
) {
    fun render() {
        renderCollisionShapes()
        renderAiPathNodes()
    }

    private fun renderAiPathNodes() {
        if (!assetsManager.gameSettings.aiShowPathNodes) return

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.transformMatrix = auxMatrix.idt()
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST)
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        for (entity in groundAiCharacterEntities) {
            val aiComponent = ComponentsMapper.groundCharacterAi.get(entity)
            aiComponent.path.nodes.forEach { node ->
                shapeRenderer.box(node.x + 0.5F, 0.1F, node.y + 0.5F, 1F, 1F, 1F)
            }
        }
        shapeRenderer.end()
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST)
    }

    private fun renderCollisionShapes() {
        if (!assetsManager.gameSettings.showCollisionShapes) return

        val debugDrawingMethod: CollisionShapesDebugDrawing? = debugDrawingMethod
        debugDrawingMethod?.drawCollisionShapes(camera)
    }

}
