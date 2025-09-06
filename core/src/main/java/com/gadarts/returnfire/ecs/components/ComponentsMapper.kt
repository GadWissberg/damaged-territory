package com.gadarts.returnfire.ecs.components

import com.badlogic.ashley.core.ComponentMapper
import com.gadarts.returnfire.ecs.components.ai.*
import com.gadarts.returnfire.ecs.components.amb.AmbAnimationComponent
import com.gadarts.returnfire.ecs.components.amb.AmbComponent
import com.gadarts.returnfire.ecs.components.amb.AmbCorpsePart
import com.gadarts.returnfire.ecs.components.amb.AmbSoundComponent
import com.gadarts.returnfire.ecs.components.arm.PrimaryArmComponent
import com.gadarts.returnfire.ecs.components.arm.SecondaryArmComponent
import com.gadarts.returnfire.ecs.components.bullet.BulletComponent
import com.gadarts.returnfire.ecs.components.cd.ChildDecalComponent
import com.gadarts.returnfire.ecs.components.effects.*
import com.gadarts.returnfire.ecs.components.model.ModelInstanceComponent
import com.gadarts.returnfire.ecs.components.onboarding.BoardingComponent
import com.gadarts.returnfire.ecs.components.physics.GhostPhysicsComponent
import com.gadarts.returnfire.ecs.components.physics.PhysicsComponent
import com.gadarts.returnfire.ecs.components.pit.ElevatorComponent
import com.gadarts.returnfire.ecs.components.pit.ElevatorDoorComponent
import com.gadarts.returnfire.ecs.components.turret.TurretBaseComponent
import com.gadarts.returnfire.ecs.components.turret.TurretCannonComponent
import com.gadarts.returnfire.ecs.components.turret.TurretComponent

object ComponentsMapper {
    val modelInstance: ComponentMapper<ModelInstanceComponent> =
        ComponentMapper.getFor(ModelInstanceComponent::class.java)
    val childDecal: ComponentMapper<ChildDecalComponent> =
        ComponentMapper.getFor(ChildDecalComponent::class.java)
    val ambSound: ComponentMapper<AmbSoundComponent> =
        ComponentMapper.getFor(AmbSoundComponent::class.java)
    val player: ComponentMapper<PlayerComponent> =
        ComponentMapper.getFor(PlayerComponent::class.java)
    val primaryArm: ComponentMapper<PrimaryArmComponent> =
        ComponentMapper.getFor(PrimaryArmComponent::class.java)
    val secondaryArm: ComponentMapper<SecondaryArmComponent> =
        ComponentMapper.getFor(SecondaryArmComponent::class.java)
    val bullet: ComponentMapper<BulletComponent> =
        ComponentMapper.getFor(BulletComponent::class.java)
    val amb: ComponentMapper<AmbComponent> =
        ComponentMapper.getFor(AmbComponent::class.java)
    val independentDecal: ComponentMapper<IndependentDecalComponent> =
        ComponentMapper.getFor(IndependentDecalComponent::class.java)
    val particleEffect: ComponentMapper<ParticleEffectComponent> =
        ComponentMapper.getFor(ParticleEffectComponent::class.java)
    val animatedTexture: ComponentMapper<AnimatedTextureComponent> =
        ComponentMapper.getFor(AnimatedTextureComponent::class.java)
    val physics: ComponentMapper<PhysicsComponent> =
        ComponentMapper.getFor(PhysicsComponent::class.java)
    val ai: ComponentMapper<BaseAiComponent> =
        ComponentMapper.getFor(BaseAiComponent::class.java)
    val aiTurret: ComponentMapper<AiTurretComponent> =
        ComponentMapper.getFor(AiTurretComponent::class.java)
    val character: ComponentMapper<CharacterComponent> =
        ComponentMapper.getFor(CharacterComponent::class.java)
    val spark: ComponentMapper<SparkComponent> =
        ComponentMapper.getFor(SparkComponent::class.java)
    val ground: ComponentMapper<GroundComponent> =
        ComponentMapper.getFor(GroundComponent::class.java)
    val waterWave: ComponentMapper<GroundBlastComponent> =
        ComponentMapper.getFor(GroundBlastComponent::class.java)
    val turret: ComponentMapper<TurretComponent> =
        ComponentMapper.getFor(TurretComponent::class.java)
    val turretBase: ComponentMapper<TurretBaseComponent> =
        ComponentMapper.getFor(TurretBaseComponent::class.java)
    val boarding: ComponentMapper<BoardingComponent> =
        ComponentMapper.getFor(BoardingComponent::class.java)
    val baseDoor: ComponentMapper<ElevatorDoorComponent> =
        ComponentMapper.getFor(ElevatorDoorComponent::class.java)
    val elevator: ComponentMapper<ElevatorComponent> =
        ComponentMapper.getFor(ElevatorComponent::class.java)
    val hangarStage: ComponentMapper<StageComponent> =
        ComponentMapper.getFor(StageComponent::class.java)
    val childModelInstance: ComponentMapper<ChildModelInstanceComponent> =
        ComponentMapper.getFor(ChildModelInstanceComponent::class.java)
    val autoAim: ComponentMapper<AutoAimComponent> =
        ComponentMapper.getFor(AutoAimComponent::class.java)
    val crashingAircraftEmitter: ComponentMapper<CrashingAircraftEmitter> =
        ComponentMapper.getFor(CrashingAircraftEmitter::class.java)
    val limitedVelocity: ComponentMapper<LimitedVelocityComponent> =
        ComponentMapper.getFor(LimitedVelocityComponent::class.java)
    val road: ComponentMapper<RoadComponent> =
        ComponentMapper.getFor(RoadComponent::class.java)
    val ambAnimation: ComponentMapper<AmbAnimationComponent> =
        ComponentMapper.getFor(AmbAnimationComponent::class.java)
    val ghostPhysics: ComponentMapper<GhostPhysicsComponent> =
        ComponentMapper.getFor(GhostPhysicsComponent::class.java)
    val deathSequence: ComponentMapper<DeathSequenceComponent> =
        ComponentMapper.getFor(DeathSequenceComponent::class.java)
    val ambCorpsePart: ComponentMapper<AmbCorpsePart> =
        ComponentMapper.getFor(AmbCorpsePart::class.java)
    val fence: ComponentMapper<FenceComponent> =
        ComponentMapper.getFor(FenceComponent::class.java)
    val drowningEffect: ComponentMapper<DrowningEffectComponent> =
        ComponentMapper.getFor(DrowningEffectComponent::class.java)
    val flyingPart: ComponentMapper<FlyingPartComponent> =
        ComponentMapper.getFor(FlyingPartComponent::class.java)
    val apacheAiComponent: ComponentMapper<ApacheAiComponent> =
        ComponentMapper.getFor(ApacheAiComponent::class.java)
    val groundCharacterAiComponent: ComponentMapper<GroundCharacterAiComponent> =
        ComponentMapper.getFor(GroundCharacterAiComponent::class.java)
    val turretEnemyAiComponent: ComponentMapper<TurretEnemyAiComponent> =
        ComponentMapper.getFor(TurretEnemyAiComponent::class.java)
    val frontWheelsComponent: ComponentMapper<FrontWheelsComponent> =
        ComponentMapper.getFor(FrontWheelsComponent::class.java)
    val turretCannonComponent: ComponentMapper<TurretCannonComponent> =
        ComponentMapper.getFor(TurretCannonComponent::class.java)
    val turretAutomationComponent: ComponentMapper<TurretAutomationComponent> =
        ComponentMapper.getFor(TurretAutomationComponent::class.java)
    val flag: ComponentMapper<FlagComponent> =
        ComponentMapper.getFor(FlagComponent::class.java)
}
