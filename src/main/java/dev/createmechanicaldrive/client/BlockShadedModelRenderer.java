package dev.createmechanicaldrive.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

public final class BlockShadedModelRenderer {

    private BlockShadedModelRenderer() {
    }

    public static void render(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            BlockState state,
            BakedModel model,
            float red,
            float green,
            float blue,
            int packedLight,
            int packedOverlay,
            RenderType renderType
    ) {
        RandomSource random =
                RandomSource.create();

        for (Direction direction : Direction.values()) {
            random.setSeed(42L);

            renderQuads(
                    pose,
                    consumer,
                    model.getQuads(
                            state,
                            direction,
                            random,
                            ModelData.EMPTY,
                            renderType
                    ),
                    red,
                    green,
                    blue,
                    packedLight,
                    packedOverlay
            );
        }

        random.setSeed(42L);

        renderQuads(
                pose,
                consumer,
                model.getQuads(
                        state,
                        null,
                        random,
                        ModelData.EMPTY,
                        renderType
                ),
                red,
                green,
                blue,
                packedLight,
                packedOverlay
        );
    }

    private static void renderQuads(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads,
            float red,
            float green,
            float blue,
            int packedLight,
            int packedOverlay
    ) {
        ClientLevel level =
                Minecraft.getInstance().level;

        for (net.minecraft.client.renderer.block.model.BakedQuad quad : quads) {
            float shade =
                    getShade(
                            level,
                            pose,
                            quad.getDirection(),
                            quad.isShade()
                    );

            float quadRed = shade;
            float quadGreen = shade;
            float quadBlue = shade;

            if (quad.isTinted()) {
                quadRed *= Mth.clamp(red, 0.0F, 1.0F);
                quadGreen *= Mth.clamp(green, 0.0F, 1.0F);
                quadBlue *= Mth.clamp(blue, 0.0F, 1.0F);
            }

            consumer.putBulkData(
                    pose,
                    quad,
                    quadRed,
                    quadGreen,
                    quadBlue,
                    1.0F,
                    packedLight,
                    packedOverlay
            );
        }
    }

    private static float getShade(
            ClientLevel level,
            PoseStack.Pose pose,
            Direction direction,
            boolean shade
    ) {
        if (level == null) {
            return 1.0F;
        }

        Vec3i normal =
                direction.getNormal();

        Vector3f transformedNormal =
                pose.transformNormal(
                        normal.getX(),
                        normal.getY(),
                        normal.getZ(),
                        new Vector3f()
                );

        if (transformedNormal.lengthSquared() > 1.0E-8F) {
            transformedNormal.normalize();
        }

        return level.getShade(
                transformedNormal.x(),
                transformedNormal.y(),
                transformedNormal.z(),
                shade
        );
    }
}
