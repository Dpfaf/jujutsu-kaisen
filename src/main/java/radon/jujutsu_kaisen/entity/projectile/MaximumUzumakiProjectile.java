package radon.jujutsu_kaisen.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import radon.jujutsu_kaisen.ExplosionHandler;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.capability.data.ISorcererData;
import radon.jujutsu_kaisen.capability.data.SorcererDataHandler;
import radon.jujutsu_kaisen.capability.data.sorcerer.SorcererGrade;
import radon.jujutsu_kaisen.damage.JJKDamageSources;
import radon.jujutsu_kaisen.entity.JJKEntities;
import radon.jujutsu_kaisen.entity.base.CursedSpirit;
import radon.jujutsu_kaisen.entity.base.JujutsuProjectile;
import radon.jujutsu_kaisen.util.HelperMethods;
import radon.jujutsu_kaisen.util.RotationUtil;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class MaximumUzumakiProjectile extends JujutsuProjectile implements GeoEntity {
    private static final int DELAY = 20;
    private static final double RANGE = 10.0D;
    private static final float MAX_POWER = 7.5F;

    private float power;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public MaximumUzumakiProjectile(EntityType<? extends Projectile> pType, Level pLevel) {
        super(pType, pLevel);
    }

    public MaximumUzumakiProjectile(LivingEntity owner, float power) {
        super(JJKEntities.MAXIMUM_UZUMAKI.get(), owner.level(), owner, power);

        Vec3 pos = owner.position()
                .subtract(RotationUtil.getTargetAdjustedLookAngle(owner).multiply(this.getBbWidth(), 0.0D, this.getBbWidth()))
                .add(0.0D, this.getBbHeight(), 0.0D);
        this.moveTo(pos.x, pos.y, pos.z, RotationUtil.getTargetAdjustedYRot(owner), RotationUtil.getTargetAdjustedXRot(owner));

        if (!owner.getCapability(SorcererDataHandler.INSTANCE).isPresent()) return;

        ISorcererData cap = owner.getCapability(SorcererDataHandler.INSTANCE).resolve().orElseThrow();

        if (this.level() instanceof ServerLevel level) {
            for (Entity entity : cap.getSummons()) {
                if (this.power == MAX_POWER) break;
                if (!(entity instanceof CursedSpirit curse)) continue;

                if (curse.getGrade().ordinal() >= SorcererGrade.SEMI_GRADE_1.ordinal() && curse.getTechnique() != null) cap.absorb(curse.getTechnique());

                this.power = Math.min(MAX_POWER, this.power + HelperMethods.getPower(curse.getExperience()));
                curse.discard();
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);

        pCompound.putFloat("power", this.power);
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);

        this.power = pCompound.getFloat("power");
    }

    @Override
    public void tick() {
        super.tick();

        if (this.getOwner() instanceof LivingEntity owner) {
            if (this.level().isClientSide) return;

            if (this.getTime() < DELAY) {
                if (!owner.isAlive()) {
                    this.discard();
                } else {
                    Vec3 pos = owner.position()
                            .subtract(RotationUtil.getTargetAdjustedLookAngle(owner).multiply(this.getBbWidth(), 0.0D, this.getBbWidth()))
                            .add(0.0D, this.getBbHeight(), 0.0D);
                    this.moveTo(pos.x, pos.y, pos.z, RotationUtil.getTargetAdjustedYRot(owner), RotationUtil.getTargetAdjustedXRot(owner));
                }
            } else if (this.getTime() - 20 >= DELAY) {
                this.discard();
            } else if (this.getTime() == DELAY) {
                Vec3 start = owner.getEyePosition();
                Vec3 look = RotationUtil.getTargetAdjustedLookAngle(owner);
                Vec3 end = start.add(look.scale(RANGE));
                HitResult result = RotationUtil.getHitResult(owner, start, end);

                Vec3 pos = result.getType() == HitResult.Type.MISS ? end : result.getLocation();
                this.setPos(pos);

                Vec3 offset = new Vec3(this.getX(), this.getY() + (this.getBbHeight() / 2.0F), this.getZ());
                ExplosionHandler.spawn(this.level().dimension(), offset, this.power * 2.0F, 3 * 20, this.getPower() * 0.5F, owner,
                        JJKDamageSources.indirectJujutsuAttack(this, owner, JJKAbilities.MAXIMUM_UZUMAKI.get()), false);
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
