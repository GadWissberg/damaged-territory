package com.gadarts.returnfire.systems.ai

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.ai.msg.Telegram
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import com.gadarts.returnfire.assets.definitions.ModelDefinition
import com.gadarts.returnfire.assets.definitions.ParticleEffectDefinition
import com.gadarts.returnfire.assets.definitions.SoundDefinition
import com.gadarts.returnfire.components.AiComponent
import com.gadarts.returnfire.components.ComponentsMapper
import com.gadarts.returnfire.components.TurretBaseComponent
import com.gadarts.returnfire.components.TurretComponent
import com.gadarts.returnfire.components.character.CharacterColor
import com.gadarts.returnfire.components.model.GameModelInstance
import com.gadarts.returnfire.managers.GamePlayManagers
import com.gadarts.returnfire.model.CharacterType
import com.gadarts.returnfire.systems.GameEntitySystem
import com.gadarts.returnfire.systems.HandlerOnEvent
import com.gadarts.returnfire.systems.data.GameSessionData
import com.gadarts.returnfire.systems.events.SystemEvents
import com.gadarts.returnfire.systems.events.SystemEvents.CHARACTER_OFF_BOARDED
import com.gadarts.returnfire.utils.CharacterPhysicsInitializer


class AiSystem(gamePlayManagers: GamePlayManagers) : GameEntitySystem(gamePlayManagers) {
    private val turretLogic by lazy { TurretLogic(gameSessionData, this.gamePlayManagers) }
    private val aiApacheLogic by lazy { AiApacheLogic(gameSessionData) }

    override val subscribedEvents: Map<SystemEvents, HandlerOnEvent> = mapOf(
        SystemEvents.CHARACTER_DIED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                val entity = msg.extraInfo as Entity
                val characterComponent = ComponentsMapper.character.get(entity)
                if (characterComponent.definition.getCharacterType() == CharacterType.TURRET
                    && ComponentsMapper.ai.has(entity)
                ) {
                    destroyTurret(entity, gamePlayManagers, gameSessionData)
                }
            }
        },
        SystemEvents.OPPONENT_CHARACTER_CREATED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                val entity = msg.extraInfo as Entity
                val characterComponent = ComponentsMapper.character.get(entity)
                if (characterComponent.color == CharacterColor.GREEN) {
                    gamePlayManagers.entityBuilder.addAiComponentToEntity(entity)
                }
            }
        },
        CHARACTER_OFF_BOARDED to object : HandlerOnEvent {
            override fun react(msg: Telegram, gameSessionData: GameSessionData, gamePlayManagers: GamePlayManagers) {
                CharacterPhysicsInitializer().initialize(gamePlayManagers.entityBuilder, msg.extraInfo as Entity)
            }
        },
    )

    private fun destroyTurret(
        entity: Entity,
        gamePlayManagers: GamePlayManagers,
        gameSessionData: GameSessionData
    ) {
        val modelInstanceComponent =
            ComponentsMapper.modelInstance.get(ComponentsMapper.turretBase.get(entity).turret)
        auxMatrix.set(modelInstanceComponent.gameModelInstance.modelInstance.transform)
        val transform = modelInstanceComponent.gameModelInstance.modelInstance.transform
        val position = transform.getTranslation(auxVector3_1)
        val randomDeadModel =
            if (MathUtils.randomBoolean()) ModelDefinition.TURRET_CANNON_DEAD_0 else ModelDefinition.TURRET_CANNON_DEAD_1
        modelInstanceComponent.gameModelInstance = GameModelInstance(
            ModelInstance(gamePlayManagers.assetsManager.getAssetByDefinition(randomDeadModel)),
            ModelDefinition.TURRET_CANNON_DEAD_0,
        )
        modelInstanceComponent.gameModelInstance.modelInstance.transform.set(auxMatrix)
        modelInstanceComponent.gameModelInstance.setBoundingBox(
            gamePlayManagers.assetsManager.getCachedBoundingBox(randomDeadModel)
        )
        gamePlayManagers.entityBuilder.begin()
            .addParticleEffectComponent(
                position,
                gameSessionData.pools.particleEffectsPools.obtain(ParticleEffectDefinition.EXPLOSION)
            )
            .finishAndAddToEngine()
        gamePlayManagers.soundPlayer.play(
            gamePlayManagers.assetsManager.getAssetByDefinition(SoundDefinition.EXPLOSION),
        )
    }


    private val enemyTurretEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(Family.all(TurretComponent::class.java, AiComponent::class.java).get())
    }

    private val aiCharacterEntities: ImmutableArray<Entity> by lazy {
        engine.getEntitiesFor(
            Family.all(AiComponent::class.java).exclude(TurretComponent::class.java, TurretBaseComponent::class.java)
                .get()
        )
    }


    override fun update(deltaTime: Float) {
        if (gameSessionData.hudData.console.isActive || gameSessionData.gamePlayData.player == null) return

        for (turret in enemyTurretEntities) {
            val characterComponent = ComponentsMapper.character.get(ComponentsMapper.turret.get(turret).base)
            if (characterComponent.dead || characterComponent.deathSequenceDuration > 0) continue

            turretLogic.attack(deltaTime, turret)
        }
        for (character in aiCharacterEntities) {
            aiApacheLogic.updateCharacter(character, deltaTime)
        }
    }


    override fun resume(delta: Long) {
    }

    override fun dispose() {
    }

    companion object {
        private val auxVector3_1 = Vector3()
        private val auxMatrix = Matrix4()
    }
}