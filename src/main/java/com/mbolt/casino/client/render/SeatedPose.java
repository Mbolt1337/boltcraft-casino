package com.mbolt.casino.client.render;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.entity.SeatEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/** A render-scoped pose: standing players, mobs and other vehicles keep their original animation. */
@EventBusSubscriber(modid = CasinoMod.MODID, value = Dist.CLIENT)
public final class SeatedPose {
    private static final Set<ModelPart> LEGS = Collections.newSetFromMap(new IdentityHashMap<>());
    private static boolean active;

    private SeatedPose() {
    }

    @SubscribeEvent
    public static void beforePlayer(RenderPlayerEvent.Pre event) {
        LEGS.clear();
        active = event.getEntity().getVehicle() instanceof SeatEntity;
    }

    @SubscribeEvent
    public static void afterPlayer(RenderPlayerEvent.Post event) {
        active = false;
        LEGS.clear();
    }

    public static void animate(HumanoidModel<?> model, LivingEntity entity) {
        if (active && entity.getVehicle() instanceof SeatEntity) {
            register(model);
        }
    }

    public static void copy(HumanoidModel<?> source, HumanoidModel<?> target) {
        if (bends(source.rightLeg)) {
            register(target);
        }
    }

    private static void register(HumanoidModel<?> model) {
        for (ModelPart leg : new ModelPart[]{model.leftLeg, model.rightLeg}) {
            leg.xRot = (float) (-Math.PI / 2);
            leg.yRot = 0;
            leg.zRot = 0;
            LEGS.add(leg);
        }
        if (model instanceof PlayerModel<?> player) {
            LEGS.add(player.leftPants);
            LEGS.add(player.rightPants);
        }
    }

    public static boolean bends(ModelPart part) {
        return active && LEGS.contains(part);
    }
}
