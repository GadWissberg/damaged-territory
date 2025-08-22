package com.gadarts.returnfire.screens.hangar

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Disposable
import com.gadarts.returnfire.managers.SoundManager
import com.gadarts.returnfire.screens.ScreensManager
import com.gadarts.shared.GameAssetManager
import com.gadarts.shared.assets.definitions.SoundDefinition
import com.gadarts.shared.data.definitions.CharacterDefinition

class HangarSceneHandler(
    private val soundManager: SoundManager,
    private val assetsManager: GameAssetManager,
    private val screenManager: ScreensManager
) :
    Disposable {
    private var elevatorSoundMoveSoundId: Long = -1
    private lateinit var hangarScreenMenu: HangarScreenMenu
    private val sceneModels = HangarSceneModelsData(assetsManager)
    private val hangarSceneLightingData = HangarSceneLightingData()
    private var deployingState = 0
    private var selected: VehicleStage? = null
    private var swingTime: Float = 0.0f

    fun render(delta: Float) {
        if (selected != null) {
            val reachedDestination = selected!!.updateLocation(delta, deployingState)
            if (reachedDestination) {
                if (elevatorSoundMoveSoundId > -1) {
                    soundManager.stop(
                        assetsManager.getAssetByDefinition(SoundDefinition.STAGE_MOVE),
                        elevatorSoundMoveSoundId
                    )
                    elevatorSoundMoveSoundId = -1
                    soundManager.play(
                        assetsManager.getAssetByDefinition(SoundDefinition.STAGE_DEPLOY)
                    )
                }
                if (deployingState > 0) {
                    screenManager.goToWarScreen(
                        selected!!.characterDefinition,
                        hangarScreenMenu.isAutoAimSelected()
                    )
                } else {
                    selected = null
                    hangarScreenMenu.show()
                }
            }
        }
        animateHook(delta)
        sceneModels.hangarAmbModels.fanModelInstance.transform.rotate(Vector3.Y, 320F * delta)
        sceneModels.hangarAmbModels.ceilingFanModelInstance.transform.rotate(Vector3.Y, 160F * delta)
        sceneModels.camera.update()
        sceneModels.charactersModels.vehicles.forEach { vehicle ->
            vehicle.updateLocation()
        }
        renderShadows(hangarSceneLightingData.shadowLight, hangarSceneLightingData.shadowBatch)
        sceneModels.batch.begin(sceneModels.camera)
        renderModels(hangarSceneLightingData.environment)
        sceneModels.batch.end()
    }

    private fun renderVehicle(selectableVehicle: SelectableVehicle) {
        sceneModels.batch.render(selectableVehicle.modelInstance, hangarSceneLightingData.environment)
        selectableVehicle.children.forEach {
            sceneModels.batch.render(
                it.modelInstance,
                hangarSceneLightingData.environment
            )
        }
    }

    private fun initializeEnvironment() {
        hangarSceneLightingData.shadowLight.set(0.2F, 0.2F, 0.2F, 0F, -1F, -0.01F)
        hangarSceneLightingData.environment.set(
            ColorAttribute(
                ColorAttribute.AmbientLight,
                Color(0.7F, 0.7F, 0.7F, 1F)
            )
        )
        hangarSceneLightingData.environment.add(hangarSceneLightingData.shadowLight)
        hangarSceneLightingData.environment.shadowMap = hangarSceneLightingData.shadowLight
    }

    fun returnFromCombat() {
        elevatorSoundMoveSoundId = soundManager.play(assetsManager.getAssetByDefinition(SoundDefinition.STAGE_MOVE))
        deployingState = -1
    }

    private fun renderModels(environment: Environment) {
        sceneModels.batch.render(sceneModels.hangarAmbModels.sceneModelInstance, environment)
        sceneModels.batch.render(sceneModels.hangarAmbModels.hookModelInstance, environment)
        sceneModels.batch.render(sceneModels.hangarAmbModels.fanModelInstance, environment)
        sceneModels.batch.render(sceneModels.stagesModels.stageTopLeftModelInstance.modelInstance, environment)
        sceneModels.batch.render(sceneModels.stagesModels.stageTopRightModelInstance.modelInstance, environment)
        sceneModels.batch.render(sceneModels.stagesModels.stageTank.modelInstance, environment)
        sceneModels.batch.render(sceneModels.stagesModels.stageApache.modelInstance, environment)
        sceneModels.batch.render(sceneModels.stagesModels.stageJeep.modelInstance, environment)
        sceneModels.batch.render(sceneModels.stagesModels.stageBottomRightModelInstance.modelInstance, environment)
        sceneModels.charactersModels.vehicles.forEach { vehicle ->
            renderVehicle(vehicle)
        }
    }

    private fun renderShadows(
        directionalShadowLight: DirectionalShadowLight,
        modelBatch: ModelBatch
    ) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        directionalShadowLight.begin(
            auxVector.set(sceneModels.camera.position).add(-2F, 0F, -4F),
            sceneModels.camera.direction
        )
        modelBatch.begin(directionalShadowLight.camera)
        modelBatch.render(sceneModels.hangarAmbModels.sceneModelInstance)
        modelBatch.render(sceneModels.hangarAmbModels.hookModelInstance)
        modelBatch.render(sceneModels.hangarAmbModels.fanModelInstance)
        modelBatch.render(sceneModels.stagesModels.stageJeep.modelInstance)
        modelBatch.render(sceneModels.stagesModels.stageTopRightModelInstance.modelInstance)
        modelBatch.render(sceneModels.stagesModels.stageTank.modelInstance)
        modelBatch.render(sceneModels.stagesModels.stageApache.modelInstance)
        modelBatch.render(sceneModels.stagesModels.stageJeep.modelInstance)
        modelBatch.render(sceneModels.stagesModels.stageBottomRightModelInstance.modelInstance)
        modelBatch.render(sceneModels.hangarAmbModels.ceilingModelInstance)
        modelBatch.render(sceneModels.hangarAmbModels.ceilingFanModelInstance)
        modelBatch.end()
        directionalShadowLight.end()
    }

    private fun animateHook(delta: Float) {
        swingTime += delta * 0.5F
        val progress = (MathUtils.sin(swingTime * MathUtils.PI) + 1) / 2
        val swingAngleZ: Float = Interpolation.fade.apply(-0.03F, 0.03F, progress)
        val swingAngleY: Float = Interpolation.fade.apply(-0.2F, 0.2F, progress)
        val originalTransform: Matrix4 = sceneModels.hangarAmbModels.hookModelInstance.transform
        sceneModels.hangarAmbModels.hookModelInstance.transform.set(
            auxMatrix.idt()
                .set(originalTransform)
                .rotate(0f, 0f, 1f, swingAngleZ)
                .rotate(0F, 1F, 0F, swingAngleY)
        )
    }

    fun selectCharacter(character: CharacterDefinition) {
        selected = sceneModels.stagesModels.mapping[character]
        deployingState = 1
        soundManager.play(assetsManager.getAssetByDefinition(SoundDefinition.STAGE_DEPLOY))
        soundManager.play(assetsManager.getAssetByDefinition(SoundDefinition.STAGE_MOVE))
    }

    override fun dispose() {
        sceneModels.batch.dispose()
        hangarSceneLightingData.shadowLight.dispose()
        hangarSceneLightingData.shadowBatch.dispose()
    }

    private fun initializeCamera() {
        sceneModels.camera.position.set(0F, 8.7F, 13.5F)
        sceneModels.camera.lookAt(0F, 0F, 0F)
        sceneModels.camera.rotate(Vector3.X, -10F)
    }

    fun init(hangarScreenMenu: HangarScreenMenu) {
        this.hangarScreenMenu = hangarScreenMenu
        initializeCamera()
        initializeEnvironment()
    }

    companion object {
        private val auxVector = Vector3()
        private val auxMatrix = Matrix4()
    }
}
