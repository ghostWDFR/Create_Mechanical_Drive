package dev.createmechanicaldrive.content.rigid_link;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;

public class RigidLinkItem extends Item {
    private final RigidLinkJointBlockEntity.LinkType linkType;

    public RigidLinkItem(Properties properties,
                         RigidLinkJointBlockEntity.LinkType linkType) {
        super(properties);
        this.linkType = linkType;
    }

    public RigidLinkJointBlockEntity.LinkType getLinkType() {
        return linkType;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof RigidLinkJointBlockEntity
                ? InteractionResult.SUCCESS
                : InteractionResult.PASS;
    }
}
