package dev.createmechanicaldrive.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.createmechanicaldrive.CreateMechanicalDrive;
import dev.createmechanicaldrive.content.suspension_strut.CompactStrutSupportShapes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

@EventBusSubscriber(
        modid = CreateMechanicalDrive.MOD_ID,
        value = Dist.CLIENT
)
public final class CompactStrutHighlightHandler {
    private CompactStrutHighlightHandler() {
    }

    @SubscribeEvent
    public static void renderJointOnly(RenderHighlightEvent.Block event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        BlockHitResult hit = event.getTarget();
        BlockPos supportPos = hit.getBlockPos();
        VoxelShape jointShape =
                CompactStrutSupportShapes.getHitSupportJointShape(
                        level,
                        supportPos,
                        hit.getLocation()
                );
        if (jointShape == null || jointShape.isEmpty()) {
            return;
        }

        event.setCanceled(true);
        Vec3 cameraPosition = event.getCamera().getPosition();
        AABB outline = jointShape.bounds().move(
                supportPos.getX() - cameraPosition.x,
                supportPos.getY() - cameraPosition.y,
                supportPos.getZ() - cameraPosition.z
        );
        VertexConsumer consumer = event.getMultiBufferSource()
                .getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(
                event.getPoseStack(),
                consumer,
                outline,
                0.0F,
                0.0F,
                0.0F,
                0.4F
        );
    }
}
