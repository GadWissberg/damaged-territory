package com.gadarts.returnfire.components

import com.badlogic.ashley.core.ComponentMapper
import com.gadarts.returnfire.components.arm.PrimaryArmComponent
import com.gadarts.returnfire.components.arm.SecondaryArmComponent
import com.gadarts.returnfire.components.bullet.BulletComponent
import com.gadarts.returnfire.components.cd.ChildDecalComponent
import com.gadarts.returnfire.components.model.ModelInstanceComponent
import com.gadarts.returnfire.components.physics.PhysicsComponent

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
    val independentParticleEffect: ComponentMapper<IndependentParticleEffectComponent> =
        ComponentMapper.getFor(IndependentParticleEffectComponent::class.java)
    val followerParticleEffect: ComponentMapper<FollowerParticleEffectComponent> =
        ComponentMapper.getFor(FollowerParticleEffectComponent::class.java)
    val animatedTexture: ComponentMapper<AnimatedTextureComponent> =
        ComponentMapper.getFor(AnimatedTextureComponent::class.java)
    val physics: ComponentMapper<PhysicsComponent> =
        ComponentMapper.getFor(PhysicsComponent::class.java)
    val enemy: ComponentMapper<EnemyComponent> =
        ComponentMapper.getFor(EnemyComponent::class.java)
    val spark: ComponentMapper<SparkComponent> =
        ComponentMapper.getFor(SparkComponent::class.java)
    val ground: ComponentMapper<GroundComponent> =
        ComponentMapper.getFor(GroundComponent::class.java)
    val waterWave: ComponentMapper<WaterWaveComponent> =
        ComponentMapper.getFor(WaterWaveComponent::class.java)

}
