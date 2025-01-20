package com.gadarts.returnfire.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.ai.msg.MessageDispatcher
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.*
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.gadarts.returnfire.DamagedTerritory
import com.gadarts.returnfire.GameDebugSettings
import com.gadarts.returnfire.assets.definitions.FontDefinition
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.console.ConsoleImpl
import com.gadarts.returnfire.managers.GameAssetManager
import com.gadarts.returnfire.managers.SoundPlayer
import com.gadarts.returnfire.model.definitions.SimpleCharacterDefinition
import com.gadarts.returnfire.model.definitions.TurretCharacterDefinition
import com.gadarts.returnfire.screens.hangar.SelectableVehicle
import com.gadarts.returnfire.screens.hangar.SelectableVehicleChild
import com.gadarts.returnfire.screens.hangar.VehicleStage
import com.gadarts.returnfire.utils.GeneralUtils


class HangarScreen(
    private val assetsManager: GameAssetManager,
    dispatcher: MessageDispatcher,
    private val screenManager: ScreensManager,
    private val soundPlayer: SoundPlayer,
    private val runsOnMobile: Boolean,
) : Screen {
    private val aimButtonGroup: ButtonGroup<TextButton> = ButtonGroup()
    private var deployingState = 0
    private var initialized: Boolean = false
    private var selected: VehicleStage? = null
    private val buttonsTable: Table by lazy {
        createTable()
    }
    private val textTable: Table by lazy {
        createTable()
    }

    private fun createTable(): Table {
        val table = Table()
        table.setFillParent(true)
        table.debug(if (GameDebugSettings.UI_DEBUG) Table.Debug.all else Table.Debug.none)
        return table
    }

    private val tankButton: TextButton by lazy { addVehicleButton("Tank") }
    private val apacheButton: TextButton by lazy { addVehicleButton("Apache") }
    private val shadowBatch = ModelBatch(DepthShaderProvider())
    private var swingTime: Float = 0.0f
    private val sceneModelInstance by lazy {
        ModelInstance(
            assetsManager.getAssetByDefinition(
                ModelDefinition.SCENE
            )
        )
    }
    private val hookModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.HOOK))
        modelInstance.transform.setToTranslation(Vector3(-1.5F, 5.2F, 4.3F))
        modelInstance
    }
    private val fanModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.FAN))
        modelInstance.transform.setToTranslation(Vector3(0.9F, 0.1F, 4.4F))
        modelInstance
    }
    private val ceilingFanModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.FAN))
        modelInstance.transform.setToTranslation(Vector3(-1.1F, 11F, 3.3F)).scl(4.5F)
        modelInstance
    }
    private val stageTopLeftModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(-3F, 0F, 3F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, initialPosition)
    }
    private val stageTopRightModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(3F, 0F, 3F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, initialPosition)
    }
    private val stageTank by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(-3F, 0F, 6F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, initialPosition)
    }
    private val stageApache by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(3F, 0F, 6F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, initialPosition)
    }
    private val stageBottomLeftModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(-3F, 0F, 9F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, initialPosition)
    }
    private val stageBottomRightModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.STAGE))
        val initialPosition = Vector3(3F, 0F, 9F)
        modelInstance.transform.setToTranslation(initialPosition)
        VehicleStage(modelInstance, initialPosition)
    }
    private val ceilingModelInstance by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.CEILING))
        modelInstance.transform.setToTranslation(Vector3(0.9F, 11F, 5F))
        modelInstance
    }
    private val tank by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TANK_BODY))
        val vehicle = SelectableVehicle(modelInstance, stageTank.modelInstance, 1.07F, -45F)
        vehicle.addChild(
            SelectableVehicleChild(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TANK_TURRET)),
                Vector3(-0.05F, 0.2F, 0F)
            )
        )
        vehicle.addChild(
            SelectableVehicleChild(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.TANK_CANNON)),
                Vector3(0.25F, 0.17F, 0F)
            )
        )
        vehicle
    }
    private val apache by lazy {
        val modelInstance =
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.APACHE))
        val vehicle = SelectableVehicle(modelInstance, stageApache.modelInstance, 1.27F, 215F)
        vehicle.addChild(
            SelectableVehicleChild(
                ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.PROPELLER)),
                Vector3(0F, -0.02F, 0F)
            )
        )
        vehicle
    }
    private val console = ConsoleImpl(assetsManager, dispatcher)
    private val batch by lazy { ModelBatch() }
    private val camera by lazy { GeneralUtils.createCamera(55F) }
    private val environment: Environment by lazy { Environment() }

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
    private val stage = Stage()

    override fun show() {
        (Gdx.input.inputProcessor as InputMultiplexer).addProcessor(stage)
        if (initialized) {
            soundPlayer.play(assetsManager.getAssetByDefinition(SoundDefinition.STAGE_MOVE))
            deployingState = -1
        } else {
            initialize()
        }
    }

    private fun initialize() {
        initialized = true
        initializeCamera()
        initializeEnvironment()
        buttonsTable.add(tankButton)
        buttonsTable.add(apacheButton)
        buttonsTable.bottom()
        stage.addActor(buttonsTable)
        textTable.add(Image(assetsManager.getTexture("logo"))).expandX().left().row()
        textTable.pad(20F).add(
            Label(
                DamagedTerritory.VERSION,
                Label.LabelStyle(
                    assetsManager.getAssetByDefinition(FontDefinition.WOK_STENCIL),
                    Color.WHITE
                )
            )
        ).top().left().row()
        val desktopText =
            "Arrows - Movement,\nCTRL - Primary attack,\nSHIFT - Secondary attack/return to base,\n" +
                "ALT + LEFT/RIGHT - Rotate turret/strafe,\nENTER - Switch ground/air aim,\n'~' - Open console"
        val androidText =
            "An on-screen game-pad will appear in-game"
        addDescription(if (runsOnMobile) androidText else desktopText)
        addDescription("Select aiming:")
        addAimingOptions()
        stage.addActor(textTable)
        stage.addActor(console)
        console.toFront()
    }

    private fun addAimingOptions() {
        val buttonsTable = Table()
        addAimingButton(AIM_BUTTON_AUTOAIM, aimButtonGroup, buttonsTable)
        addAimingButton(AIM_BUTTON_MANUAL, aimButtonGroup, buttonsTable)
        textTable.add(buttonsTable).expand().top().left()
        aimButtonGroup.setChecked(AIM_BUTTON_AUTOAIM)
        aimButtonGroup.setMaxCheckCount(1)
        aimButtonGroup.setMinCheckCount(1)
    }

    private fun addAimingButton(
        text: String,
        buttonGroup: ButtonGroup<TextButton>,
        buttonsTable: Table
    ): Cell<TextButton> {
        val button = TextButton(
            text,
            TextButton.TextButtonStyle(
                TextureRegionDrawable(assetsManager.getTexture("button_up")),
                TextureRegionDrawable(assetsManager.getTexture("button_down")),
                TextureRegionDrawable(assetsManager.getTexture("button_checked")),
                assetsManager.getAssetByDefinition(FontDefinition.WOK_STENCIL)
            )
        )
        val cell = buttonsTable.add(button).top().left().size(100F, 100F).pad(10F)
        buttonGroup.add(button)
        return cell
    }

    private fun addDescription(text: String) {
        textTable.add(
            Label(
                text,
                Label.LabelStyle(
                    assetsManager.getAssetByDefinition(FontDefinition.CONSOLA),
                    Color.WHITE
                )
            )
        ).expandX().top().left().row()
    }

    private fun initializeEnvironment() {
        shadowLight.set(0.2F, 0.2F, 0.2F, 0F, -1F, -0.01F)
        environment.set(
            ColorAttribute(
                ColorAttribute.AmbientLight,
                Color(0.7F, 0.7F, 0.7F, 1F)
            )
        )
        environment.add(shadowLight)
        environment.shadowMap = shadowLight
    }

    private fun initializeCamera() {
        camera.position.set(0F, 8.7F, 13.5F)
        camera.lookAt(0F, 0F, 0F)
        camera.rotate(Vector3.X, -10F)
    }

    private fun addVehicleButton(text: String): TextButton {
        val textButton = TextButton(
            text,
            TextButton.TextButtonStyle(
                TextureRegionDrawable(assetsManager.getTexture("button_up")),
                TextureRegionDrawable(assetsManager.getTexture("button_down")),
                null,
                assetsManager.getAssetByDefinition(FontDefinition.WOK_STENCIL)
            )
        )
        textButton.addListener(onClick(text))
        return textButton
    }

    private fun onClick(text: String) = object : ClickListener() {
        override fun clicked(event: InputEvent?, x: Float, y: Float) {
            when (text) {
                "Tank" -> {
                    buttonsTable.remove()
                    selected = stageTank
                    deployingState = 1
                }

                "Apache" -> {
                    buttonsTable.remove()
                    selected = stageApache
                    deployingState = 1
                }
            }
            soundPlayer.play(assetsManager.getAssetByDefinition(SoundDefinition.STAGE_DEPLOY))
            soundPlayer.play(assetsManager.getAssetByDefinition(SoundDefinition.STAGE_MOVE))
        }
    }


    override fun render(delta: Float) {
        if (Gdx.input.isKeyPressed(Input.Keys.BACK) || Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            Gdx.app.exit()
        }
        if (selected != null) {
            val reachedDestination = selected!!.updateLocation(delta, deployingState)
            if (reachedDestination) {
                if (deployingState > 0) {
                    screenManager.goToWarScreen(
                        if (selected == stageTank) TurretCharacterDefinition.TANK else SimpleCharacterDefinition.APACHE,
                        aimButtonGroup.checked.text.toString() == AIM_BUTTON_AUTOAIM
                    )
                } else {
                    selected = null
                    stage.addActor(buttonsTable)
                }
            }
        }
        stage.act()
        animateHook(delta)
        fanModelInstance.transform.rotate(Vector3.Y, 320F * delta)
        ceilingFanModelInstance.transform.rotate(Vector3.Y, 160F * delta)
        camera.update()
        tank.updateLocation()
        apache.updateLocation()
        renderShadows(shadowLight, shadowBatch)
        batch.begin(camera)
        renderModels(environment)
        batch.end()
        stage.draw()
    }


    private fun renderModels(environment: Environment) {
        batch.render(sceneModelInstance, environment)
        batch.render(hookModelInstance, environment)
        batch.render(fanModelInstance, environment)
        batch.render(stageTopLeftModelInstance.modelInstance, environment)
        batch.render(stageTopRightModelInstance.modelInstance, environment)
        batch.render(stageTank.modelInstance, environment)
        batch.render(stageApache.modelInstance, environment)
        batch.render(stageBottomLeftModelInstance.modelInstance, environment)
        batch.render(stageBottomRightModelInstance.modelInstance, environment)
        renderVehicle(tank)
        renderVehicle(apache)
    }

    private fun renderVehicle(selectableVehicle: SelectableVehicle) {
        batch.render(selectableVehicle.modelInstance, environment)
        selectableVehicle.children.forEach { batch.render(it.modelInstance, environment) }
    }

    private fun renderShadows(
        directionalShadowLight: DirectionalShadowLight,
        modelBatch: ModelBatch
    ) {
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        directionalShadowLight.begin(
            auxVector.set(camera.position).add(-2F, 0F, -4F),
            camera.direction
        )
        modelBatch.begin(directionalShadowLight.camera)
        modelBatch.render(sceneModelInstance)
        modelBatch.render(hookModelInstance)
        modelBatch.render(fanModelInstance)
        modelBatch.render(stageTopLeftModelInstance.modelInstance)
        modelBatch.render(stageTopRightModelInstance.modelInstance)
        modelBatch.render(stageTank.modelInstance)
        modelBatch.render(stageApache.modelInstance)
        modelBatch.render(stageBottomLeftModelInstance.modelInstance)
        modelBatch.render(stageBottomRightModelInstance.modelInstance)
        modelBatch.render(ceilingModelInstance)
        modelBatch.render(ceilingFanModelInstance)
        modelBatch.end()
        directionalShadowLight.end()
    }

    private fun animateHook(delta: Float) {
        swingTime += delta * 0.5F
        val progress = (MathUtils.sin(swingTime * MathUtils.PI) + 1) / 2
        val swingAngleZ: Float = Interpolation.fade.apply(-0.03F, 0.03F, progress)
        val swingAngleY: Float = Interpolation.fade.apply(-0.2F, 0.2F, progress)
        val originalTransform: Matrix4 = hookModelInstance.transform
        hookModelInstance.transform.set(
            auxMatrix.idt()
                .set(originalTransform)
                .rotate(0f, 0f, 1f, swingAngleZ)
                .rotate(0F, 1F, 0F, swingAngleY)
        )
    }


    override fun resize(width: Int, height: Int) {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun hide() {
        val inputMultiplexer = Gdx.input.inputProcessor as InputMultiplexer
        inputMultiplexer.removeProcessor(console)
        inputMultiplexer.removeProcessor(stage)
    }

    override fun dispose() {
        batch.dispose()
        shadowLight.dispose()
        shadowBatch.dispose()
        stage.dispose()
    }

    companion object {
        private val auxVector = Vector3()
        private val auxMatrix = Matrix4()
        private const val AIM_BUTTON_AUTOAIM = "Auto-Aim"
        private const val AIM_BUTTON_MANUAL = "Manual"
    }
}
