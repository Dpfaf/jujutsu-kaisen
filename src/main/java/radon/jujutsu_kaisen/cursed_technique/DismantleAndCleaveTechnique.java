package radon.jujutsu_kaisen.cursed_technique;

import org.jetbrains.annotations.Nullable;
import radon.jujutsu_kaisen.ability.JJKAbilities;
import radon.jujutsu_kaisen.ability.base.IAttack;
import radon.jujutsu_kaisen.ability.base.IChanneled;
import radon.jujutsu_kaisen.ability.base.Ability;
import radon.jujutsu_kaisen.ability.base.ICharged;
import radon.jujutsu_kaisen.ability.base.IDomainAttack;
import radon.jujutsu_kaisen.ability.base.IDurationable;
import radon.jujutsu_kaisen.ability.base.ITenShadowsAttack;
import radon.jujutsu_kaisen.ability.base.IToggled;
import radon.jujutsu_kaisen.cursed_technique.base.ICursedTechnique;

import java.util.LinkedHashSet;
import java.util.Set;

public class DismantleAndCleaveTechnique implements ICursedTechnique {
    private static final Set<Ability> ABILITIES = new LinkedHashSet<>();

    static {
        ABILITIES.add(JJKAbilities.DISMANTLE.get());
        ABILITIES.add(JJKAbilities.BIG_DISMANTLE.get());
        ABILITIES.add(JJKAbilities.DISMANTLE_NET.get());
        ABILITIES.add(JJKAbilities.DISMANTLE_SKATING.get());
        ABILITIES.add(JJKAbilities.CLEAVE.get());
        ABILITIES.add(JJKAbilities.SPIDERWEB.get());
        ABILITIES.add(JJKAbilities.FIRE_ARROW.get());
    }

    @Override
    public @Nullable Ability getDomain() {
        return JJKAbilities.MALEVOLENT_SHRINE.get();
    }

    @Override
    public Set<Ability> getAbilities() {
        return new LinkedHashSet<>(ABILITIES);
    }
}
