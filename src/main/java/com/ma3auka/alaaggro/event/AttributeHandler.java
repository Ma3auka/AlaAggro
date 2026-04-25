package com.ma3auka.alaaggro.event;

import com.ma3auka.alaaggro.AlaAggro;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;

/**
 * Registers ATTACK_DAMAGE attribute on every LivingEntity type that does not already have one.
 * Per-instance value is set in MobAggroEventHandler via EntityJoinLevelEvent.
 *
 * Registered manually on the mod event bus from {@link AlaAggro}'s constructor —
 * EntityAttributeModificationEvent fires on the mod bus, not the game bus.
 */
public final class AttributeHandler {
    private AttributeHandler() {}

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void onAttributeModify(EntityAttributeModificationEvent event) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!isLivingType(type)) continue;
            EntityType living = type;
            try {
                if (!event.has(living, Attributes.ATTACK_DAMAGE)) {
                    event.add(living, Attributes.ATTACK_DAMAGE, 0.0);
                }
            } catch (IllegalArgumentException ignored) {
                // type does not have a registered attribute supplier — skip silently
            }
        }
    }

    private static boolean isLivingType(EntityType<?> type) {
        Class<?> clazz = type.getBaseClass();
        return clazz != null && LivingEntity.class.isAssignableFrom(clazz);
    }
}
