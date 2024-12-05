package com.gadarts.returnfire

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.math.MathUtils.random
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.collision.btBoxShape
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject.CollisionFlags
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.collision.btCompoundShape
import com.gadarts.returnfire.assets.GameAssetManager
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.assets.definitions.external.TextureDefinition
import com.gadarts.returnfire.components.AmbComponent
import com.gadarts.returnfire.components.AnimatedTextureComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.arm.ArmComponent
import com.gadarts.returnfire.components.arm.ArmEffectsData
import com.gadarts.returnfire.components.arm.ArmProperties
import com.gadarts.returnfire.components.arm.ArmRenderData
import com.gadarts.returnfire.components.bullet.BulletBehavior
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.components.physics.PhysicsComponent
import com.gadarts.returnfire.model.*
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.map.TilesMapping

class MapInflater(
    private val gameSessionData: GameSessionData,
    private val managers: Managers,
    private val engine: Engine
) {
    private val ambEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(AmbComponent::class.java).get()
        )
    }

    fun inflate() {
        val exculdedTiles = ArrayList<Pair<Int, Int>>()
        addAmbEntities(exculdedTiles)
        addFloor(exculdedTiles)
        addCharacters()
    }

    private fun applyTransformOnAmbEntities() {
        ambEntities.forEach {
            if (!ComponentsMapper.modelInstance.has(it)) return
            val scale = ComponentsMapper.amb.get(it).getScale(auxVector1)
            val transform =
                ComponentsMapper.modelInstance.get(it).gameModelInstance.modelInstance.transform
            transform.scl(scale).rotate(Vector3.Y, ComponentsMapper.amb.get(it).rotation)
        }
    }

    private fun addAmbObject(
        position: Vector3,
        def: AmbDefinition,
        direction: Int,
        exculdedTiles: ArrayList<Pair<Int, Int>>,
    ) {
        excludeTilesUnderBase(def, position, exculdedTiles)
        if (def.placeInMiddleOfCell) {
            position.add(0.5F, 0F, 0.5F)
        }
        val gameModelInstance =
            managers.factories.gameModelInstanceFactory.createGameModelInstance(def.getModelDefinition())
        val randomScale = if (def.isRandomizeScale()) random(MIN_SCALE, MAX_SCALE) else 1F
        val entity = managers.entityBuilder.begin()
            .addModelInstanceComponent(
                gameModelInstance,
                position,
                null,
                direction.toFloat(),
            )
            .addAmbComponent(
                auxVector1.set(randomScale, randomScale, randomScale),
                if (def.isRandomizeRotation()) random(0F, 360F) else 0F,
            )
            .finishAndAddToEngine()
        addPhysicsToObject(entity, gameModelInstance, def.collisionFlags)
    }

    private fun excludeTilesUnderBase(
        def: AmbDefinition,
        position: Vector3,
        exculdedTiles: ArrayList<Pair<Int, Int>>
    ) {
        if (def == AmbDefinition.BASE) {
            val x = position.x.toInt()
            val z = position.z.toInt()
            exculdedTiles.add(Pair(x, z))
            exculdedTiles.add(Pair(x + 1, z))
            exculdedTiles.add(Pair(x, z + 1))
            exculdedTiles.add(Pair(x + 1, z + 1))
        }
    }

    private fun addCharacter(
        position: Vector3,
        characterDefinition: CharacterDefinition,
        direction: Int
    ) {
        val gameModelInstance =
            managers.factories.gameModelInstanceFactory.createGameModelInstance(characterDefinition.getModelDefinition())
        val baseEntity = managers.entityBuilder.begin()
            .addModelInstanceComponent(
                gameModelInstance,
                position,
                null,
                direction.toFloat(),
                GameDebugSettings.HIDE_ENEMIES
            )
            .addCharacterComponent(TurretCharacterDefinition.TURRET_CANNON)
            .addEnemyComponent()
            .addTurretBaseComponent()
            .finishAndAddToEngine()
        addPhysicsToObject(baseEntity, gameModelInstance, CollisionFlags.CF_STATIC_OBJECT)
        if (characterDefinition.getCharacterType() == CharacterType.TURRET) {
            addTurret(baseEntity)
        }
    }


    private fun addPhysicsToObject(
        entity: Entity,
        gameModelInstance: GameModelInstance,
        collisionFlags: Int
    ): PhysicsComponent {
        return managers.entityBuilder.addPhysicsComponentToEntity(
            entity,
            createShapeForStaticObject(gameModelInstance.definition!!),
            0F,
            managers,
            collisionFlags,
            Matrix4(gameModelInstance.modelInstance.transform),
        )
    }

    private fun createShapeForStaticObject(modelDefinition: ModelDefinition): btCollisionShape {
        val shape: btCollisionShape
        if (modelDefinition.physicalShapeCreator == null) {
            shape = btCompoundShape()
            val dimensions =
                auxBoundingBox.set(managers.assetsManager.getCachedBoundingBox(modelDefinition))
                    .getDimensions(
                        auxVector3
                    )
            val btBoxShape = btBoxShape(
                dimensions.scl(0.5F)
            )
            shape.addChildShape(
                auxMatrix.idt().translate(0F, dimensions.y / 2F, 0F), btBoxShape
            )
        } else {
            shape = modelDefinition.physicalShapeCreator.create()
        }
        return shape
    }

    private fun addTurret(
        baseEntity: Entity
    ) {
        val assetsManager = managers.assetsManager
        val modelInstance =
            managers.factories.gameModelInstanceFactory.createGameModelInstance(ModelDefinition.TURRET_CANNON)
        val spark = addTurretSpark(assetsManager, modelInstance.modelInstance)
        val turret = managers.entityBuilder.begin()
            .addEnemyComponent()
            .addModelInstanceComponent(
                modelInstance,
                calculateTurretPosition(baseEntity, assetsManager),
                null,
            )
            .addTurretComponent(baseEntity, false, null)
            .addPrimaryArmComponent(
                spark,
                createTurretArmProperties(assetsManager),
                BulletBehavior.REGULAR
            )
            .finishAndAddToEngine()
        ComponentsMapper.turretBase.get(baseEntity).turret = turret
        addPhysicsToTurret(assetsManager, turret, modelInstance.modelInstance)
    }

    private fun addPhysicsToTurret(
        assetsManager: GameAssetManager,
        turret: Entity,
        modelInstance: ModelInstance
    ) {
        val cachedBoundingBox = assetsManager.getCachedBoundingBox(ModelDefinition.TURRET_CANNON)
        val shape = btCompoundShape()
        shape.addChildShape(
            auxMatrix.idt().translate(ModelDefinition.TURRET_CANNON.boundingBoxBias), btBoxShape(
                auxVector1.set(
                    cachedBoundingBox.width / 2F,
                    cachedBoundingBox.height / 2F,
                    cachedBoundingBox.depth / 2F
                )
            )
        )
        managers.entityBuilder.addPhysicsComponentToEntity(
            turret,
            shape,
            10F,
            managers,
            CollisionFlags.CF_KINEMATIC_OBJECT,
            modelInstance.transform,
        )
    }

    private fun createTurretArmProperties(assetsManager: GameAssetManager) = ArmProperties(
        2,
        assetsManager.getAssetByDefinition(SoundDefinition.CANNON),
        3000L,
        20F,
        ArmEffectsData(
            null,
            null,
            gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.SMOKE_SMALL),
            null,
        ),
        ArmRenderData(
            ModelDefinition.CANNON_BULLET,
            assetsManager.getCachedBoundingBox(ModelDefinition.CANNON_BULLET),
        ),
        true,
        gameSessionData.pools.rigidBodyPools.obtainRigidBodyPool(ModelDefinition.CANNON_BULLET)
    )

    private fun calculateTurretPosition(
        baseEntity: Entity,
        assetsManager: GameAssetManager
    ): Vector3 {
        return ComponentsMapper.physics.get(baseEntity).rigidBody.worldTransform.getTranslation(
            auxVector1
        ).add(0F, assetsManager.getCachedBoundingBox(ModelDefinition.TURRET_BASE).height, 0F)
    }

    private fun addTurretSpark(
        assetsManager: GameAssetManager,
        modelInstance: ModelInstance
    ): Entity {
        val model = GameModelInstance(
            ModelInstance(assetsManager.getAssetByDefinition(ModelDefinition.CANNON_SPARK)),
            ModelDefinition.CANNON_SPARK,
        )
        val spark = managers.entityBuilder.begin()
            .addModelInstanceComponent(
                model,
                Vector3(),
                assetsManager.getCachedBoundingBox(ModelDefinition.TURRET_CANNON),
                hidden = true
            )
            .addSparkComponent(createRelativePositionCalculatorForTurret(modelInstance))
            .finishAndAddToEngine()
        return spark
    }

    private fun createRelativePositionCalculatorForTurret(modelInstance: ModelInstance) =
        object : ArmComponent.RelativePositionCalculator {
            override fun calculate(parent: Entity, output: Vector3): Vector3 {
                return output.setZero()
                    .add(
                        0.7F,
                        0F,
                        ComponentsMapper.turret.get(parent).updateCurrentShootingArm() * 0.3F
                    )
                    .rot(modelInstance.transform)
            }
        }


    private fun addCharacters() {
        gameSessionData.mapData.currentMap.placedElements.filter {
            val definition = it.definition
            definition.getType() == ElementType.CHARACTER
                    && definition != SimpleCharacterDefinition.APACHE
                    && definition != TurretCharacterDefinition.TANK
        }
            .forEach {
                addCharacter(
                    auxVector2.set(it.col.toFloat() + 0.5F, 0.01F, it.row.toFloat() + 0.5F),
                    it.definition as CharacterDefinition,
                    it.direction
                )
            }
    }

    private fun addAmbEntities(exculdedTiles: ArrayList<Pair<Int, Int>>) {
        gameSessionData.mapData.currentMap.placedElements.filter { it.definition.getType() == ElementType.AMB }
            .forEach {
                addAmbObject(
                    auxVector2.set(it.col.toFloat(), 0.01F, it.row.toFloat()),
                    it.definition as AmbDefinition,
                    it.direction,
                    exculdedTiles
                )
            }
        applyTransformOnAmbEntities()
    }

    private fun addFloor(exculdedTiles: ArrayList<Pair<Int, Int>>) {
        gameSessionData.renderData.modelCache.begin()
        val tilesMapping = gameSessionData.mapData.currentMap.tilesMapping
        val depth = tilesMapping.size
        val width = tilesMapping[0].size
        addFloorRegion(depth, width, exculdedTiles)
        addAllExternalSea(width, depth)
        gameSessionData.renderData.modelCache.end()
    }

    private fun addFloorTile(
        row: Int,
        col: Int,
        modelInstance: GameModelInstance,
        exculdedTiles: ArrayList<Pair<Int, Int>>
    ): Entity {
        val entity = createAndAddGroundTileEntity(
            modelInstance,
            auxVector1.set(col.toFloat() + 0.5F, 0F, row.toFloat() + 0.5F)
        )
        if (!exculdedTiles.contains(Pair(col, row))) {
            gameSessionData.renderData.modelCache.add(modelInstance.modelInstance)
        }
        val textureDefinition = applyTextureToFloorTile(col, row, entity, modelInstance)
        if (textureDefinition != null && !textureDefinition.fileName.contains("water")) {
            managers.entityBuilder.addPhysicsComponentToEntity(
                entity,
                btBoxShape(Vector3(0.5F, 0.1F, 0.5F)),
                0F,
                managers,
                CollisionFlags.CF_STATIC_OBJECT,
                modelInstance.modelInstance.transform
            )
        }
        return entity
    }

    private fun applyTextureToFloorTile(
        col: Int,
        row: Int,
        entity: Entity,
        modelInstance: GameModelInstance
    ): TextureDefinition? {
        var textureDefinition: TextureDefinition? = null
        val assetsManager = managers.assetsManager
        if (isPositionInsideBoundaries(row, col)) {
            gameSessionData.mapData.tilesEntities[row][col] = entity
            val definitions = assetsManager.getTexturesDefinitions()
            val indexOfFirst =
                TILES_CHARS.indexOfFirst { c: Char -> gameSessionData.mapData.currentMap.tilesMapping[row][col] == c }
            textureDefinition =
                definitions.definitions[TilesMapping.tiles[indexOfFirst]]
        }
        if (textureDefinition != null) {
            val texture =
                assetsManager.getTexture(textureDefinition)
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            (modelInstance.modelInstance.materials.get(0)
                .get(TextureAttribute.Diffuse) as TextureAttribute)
                .set(TextureRegion(texture))
            if (textureDefinition.animated) {
                applyAnimatedTextureComponentToFloor(textureDefinition, entity)
            }
            if (textureDefinition.fileName.contains("water")) {
                ComponentsMapper.ground.get(entity).water = true
            }
        }
        return textureDefinition
    }

    private fun createAndAddGroundTileEntity(
        modelInstance: GameModelInstance,
        position: Vector3
    ): Entity {
        return managers.entityBuilder.begin()
            .addModelInstanceComponent(modelInstance, position, null)
            .addGroundComponent()
            .finishAndAddToEngine()
    }

    private fun addExtSea(width: Int, depth: Int, x: Float, z: Float) {
        val modelInstance = GameModelInstance(ModelInstance(gameSessionData.renderData.floorModel), null)
        val entity = createAndAddGroundTileEntity(
            modelInstance,
            auxVector1.set(x, 0F, z)
        )
        modelInstance.modelInstance.transform.scl(width.toFloat(), 1F, depth.toFloat())
        val textureAttribute =
            modelInstance.modelInstance.materials.first()
                .get(TextureAttribute.Diffuse) as TextureAttribute
        initializeExternalSeaTextureAttribute(textureAttribute, width, depth)
        gameSessionData.renderData.modelCache.add(modelInstance.modelInstance)
        val texturesDefinitions = managers.assetsManager.getTexturesDefinitions()
        applyAnimatedTextureComponentToFloor(
            texturesDefinitions.definitions["tile_water"]!!,
            entity
        )
    }

    private fun addAllExternalSea(width: Int, depth: Int) {
        addExtSea(width, EXT_SIZE, width / 2F, -EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, -EXT_SIZE / 2F, -EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, depth, -width / 2F, depth / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, -width / 2F, depth + EXT_SIZE / 2F)
        addExtSea(width, EXT_SIZE, width / 2F, depth + EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, width + EXT_SIZE / 2F, depth + EXT_SIZE / 2F)
        addExtSea(EXT_SIZE, depth, width + EXT_SIZE / 2F, depth / 2F)
        addExtSea(EXT_SIZE, EXT_SIZE, width + EXT_SIZE / 2F, -depth / 2F)
    }

    private fun addFloorRegion(
        rows: Int,
        cols: Int,
        exculdedTiles: ArrayList<Pair<Int, Int>>,
    ) {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                addFloorTile(
                    row,
                    col,
                    GameModelInstance(
                        ModelInstance(gameSessionData.renderData.floorModel),
                        null,
                    ),
                    exculdedTiles
                )
            }
        }
    }

    private fun initializeExternalSeaTextureAttribute(
        textureAttribute: TextureAttribute,
        width: Int,
        depth: Int
    ) {
        textureAttribute.textureDescription.uWrap = Texture.TextureWrap.Repeat
        textureAttribute.textureDescription.vWrap = Texture.TextureWrap.Repeat
        textureAttribute.offsetU = 0F
        textureAttribute.offsetV = 0F
        textureAttribute.scaleU = width.toFloat()
        textureAttribute.scaleV = depth.toFloat()
    }

    private fun isPositionInsideBoundaries(row: Int, col: Int) = (row >= 0
            && col >= 0
            && row < gameSessionData.mapData.currentMap.tilesMapping.size
            && col < gameSessionData.mapData.currentMap.tilesMapping[0].size)

    private fun applyAnimatedTextureComponentToFloor(
        textureDefinition: TextureDefinition,
        entity: Entity
    ) {
        val frames = com.badlogic.gdx.utils.Array<Texture>()
        for (i in 0 until textureDefinition.frames) {
            frames.add(managers.assetsManager.getTexture(textureDefinition, i))
        }
        val animation = Animation(0.25F, frames)
        animation.playMode = Animation.PlayMode.NORMAL
        entity.add(AnimatedTextureComponent(animation))
    }


    companion object {
        private val auxVector1 = Vector3()
        private val auxVector2 = Vector3()
        private val auxVector3 = Vector3()
        private val auxMatrix = Matrix4()
        private val auxBoundingBox = BoundingBox()
        private const val EXT_SIZE = 48
        private val TILES_CHARS = CharArray(80) { (it + 48).toChar() }.joinToString("")
        private const val MIN_SCALE = 0.95F
        private const val MAX_SCALE = 1.05F

    }
}
