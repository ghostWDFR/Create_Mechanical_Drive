package dev.createmechanicaldrive.client.creative;

import dev.createmechanicaldrive.creative.CreativeTabSection;
import dev.createmechanicaldrive.creative.CreativeTabSections;
import dev.createmechanicaldrive.mixin.client.AbstractContainerScreenAccessor;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.ResourceLocation;

public final class CreativeTabSectionRenderer {

    private static final int BANNER_WIDTH = 162;
    private static final int BANNER_HEIGHT = 18;

    private CreativeTabSectionRenderer() {
    }

    public static void render(
            CreativeModeInventoryScreen screen,
            GuiGraphics graphics,
            int mouseX,
            int mouseY
    ) {
        AbstractContainerScreenAccessor accessor =
                (AbstractContainerScreenAccessor) screen;

        int left =
                accessor.createMechanicalDrive$getLeftPos() + 8;

        int top =
                accessor.createMechanicalDrive$getTopPos() + 17;

        int currentRow =
                CreativeTabSections.getCurrentScrollRow();

        Font font = Minecraft.getInstance().font;

        for (CreativeTabSection section
                : CreativeTabSections.sections()) {

            Integer absoluteRow =
                    CreativeTabSections
                            .bannerRows()
                            .get(section.id());

            if (absoluteRow == null) {
                continue;
            }

            int visibleRow =
                    absoluteRow - currentRow;

            if (visibleRow < 0 || visibleRow > 4) {
                continue;
            }

            int y = top + visibleRow * 18;

            boolean hovered =
                    mouseX >= left
                            && mouseX < left + BANNER_WIDTH
                            && mouseY >= y
                            && mouseY < y + BANNER_HEIGHT;

            ResourceLocation sprite =
                    getFrame(section, hovered);

            graphics.blitSprite(
                    sprite,
                    left,
                    y,
                    BANNER_WIDTH,
                    BANNER_HEIGHT
            );

            int textWidth =
                    font.width(section.title());

            graphics.fill(
                    left + 2,
                    y + 2,
                    left + textWidth + 8,
                    y + 16,
                    section.titleBackgroundColor()
            );

            graphics.drawString(
                    font,
                    section.title(),
                    left + 5,
                    y + 5,
                    section.titleColor(),
                    true
            );
        }
    }

    private static ResourceLocation getFrame(
            CreativeTabSection section,
            boolean hovered
    ) {
        if (!hovered || section.frames().size() <= 1) {
            return section.frames().getFirst();
        }

        long time = Util.getMillis();

        int frame =
                (int) (
                        time
                                / section.frameTimeMs()
                                % section.frames().size()
                );

        return section.frames().get(frame);
    }
}