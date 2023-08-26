package radon.jujutsu_kaisen.entity.base;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererGrade;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Arrays;

public abstract class SorcererEntity extends PathfinderMob implements GeoEntity, ISorcerer {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    protected SorcererEntity(EntityType<? extends PathfinderMob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);

        Arrays.fill(this.armorDropChances, 1.0F);
        Arrays.fill(this.handDropChances, 1.0F);
    }

    @Override
    public AnimatableInstanceCache animatableCacheOverride() {
        return null;
    }

    @Override
    protected void actuallyHurt(@NotNull DamageSource pDamageSource, float pDamageAmount) {
        super.actuallyHurt(pDamageSource, pDamageAmount);

        if (pDamageSource.getEntity() instanceof LivingEntity attacker && this.canAttack(attacker)) {
            this.setTarget(attacker);
        }
    }

    private boolean isInVillage() {
        HolderSet.Named<Structure> structures = this.level.registryAccess().registryOrThrow(Registries.STRUCTURE).getTag(StructureTags.VILLAGE).orElseThrow();

        boolean success = false;

        for (Holder<Structure> holder : structures) {
            if (((ServerLevel) this.level).structureManager().getStructureAt(this.blockPosition(), holder.get()) != StructureStart.INVALID_START) {
                success = true;
                break;
            }
        }
        return success;
    }

    private boolean isInFortress() {
        Structure structure = this.level.registryAccess().registryOrThrow(Registries.STRUCTURE).get(BuiltinStructures.FORTRESS);
        if (structure == null) return false;
        return ((ServerLevel) this.level).structureManager().getStructureAt(this.blockPosition(), structure) != StructureStart.INVALID_START;
    }

    @Override
    public boolean checkSpawnRules(@NotNull LevelAccessor pLevel, @NotNull MobSpawnType pSpawnReason) {
        if (pSpawnReason == MobSpawnType.NATURAL || pSpawnReason == MobSpawnType.CHUNK_GENERATION) {
            if (this.random.nextInt(Math.max(1, this.getGrade().ordinal() * (this.isCurse() ? 50 : 25) / (this.isCurse() && this.level.isNight() ? 2 : 1))) != 0) return false;

            if (this.isCurse()) {
                if (this.getGrade().ordinal() < SorcererGrade.SPECIAL_GRADE.ordinal()) {
                    if (!this.isInVillage() && !this.isInFortress()) return false;
                } else if (!this.isInFortress()) {
                    return false;
                }
            } else {
                if (!this.isInVillage()) return false;
            }
            if (pLevel.getEntitiesOfClass(this.getClass(), AABB.ofSize(this.position(), 256.0D,  32.0D, 256.0D)).size() > 0) return false;
            if (pLevel.getEntitiesOfClass(SorcererEntity.class, AABB.ofSize(this.position(), 64.0D,  16.0D, 64.0D)).size() > 0) return false;
        }
        return super.checkSpawnRules(pLevel, pSpawnReason);
    }

    @Override
    public int getMaxHeadXRot() {
        return 90;
    }

    @Override
    public void aiStep() {
        this.updateSwingTime();

        super.aiStep();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MOVEMENT_SPEED, 0.32D)
                .add(Attributes.ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, 64.0D);
    }

    @Override
    public void onAddedToWorld() {
        super.onAddedToWorld();

        this.getCapability(SorcererDataHandler.INSTANCE).ifPresent(this::init);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {}

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
