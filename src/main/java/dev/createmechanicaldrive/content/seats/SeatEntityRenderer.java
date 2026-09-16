package dev.createmechanicaldrive.content.seats;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

public class SeatEntityRenderer
        extends EntityRenderer<SeatEntity> {

    public SeatEntityRenderer(
            EntityRendererProvider.Context context
    ) {
        super(
                context
        );
    }

    @Override
    public boolean shouldRender(
            SeatEntity entity,
            Frustum camera,
            double camX,
            double camY,
            double camZ
    ) {
        return false;
    }

    @Nullable
    @Override
    public ResourceLocation getTextureLocation(
            SeatEntity entity
    ) {
        return null;
    }
}
