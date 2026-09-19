package dev.createmechanicaldrive.creative;

import dev.createmechanicaldrive.CreateMechanicalDrive;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public final class CreativeTabSections {

    public static final int COLUMNS = 9;

    private static final List<CreativeTabSection> SECTIONS =
            new ArrayList<>();

    private static final List<Supplier<? extends Item>> UNSECTIONED_ITEMS =
            new ArrayList<>();

    private static final Map<String, Integer> BANNER_ROWS =
            new LinkedHashMap<>();

    private static int currentScrollRow = 0;

    static {
        register(
                new CreativeTabSection(
                        "transmissions",

                        Component.translatable(
                                "creative_section.mechanical_drive.transmissions"
                        ),

                        0xFFFFEB8C,
                        0xAA39231C,

                        120,

                        List.of(
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/transmissions/transmissions_00"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/transmissions/transmissions_01"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/transmissions/transmissions_02"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/transmissions/transmissions_03"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/transmissions/transmissions_04"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/transmissions/transmissions_05"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/transmissions/transmissions_06"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/transmissions/transmissions_07"
                                )
                        ),

                        List.of(
                                CreateMechanicalDrive.GEARBOX_INPUT_ITEM,
                                CreateMechanicalDrive.GEARBOX_SPEED_ITEM,
                                CreateMechanicalDrive.TANK_TRANSMISSION_HOUSING_ASSEMBLED_ITEM,
                                CreateMechanicalDrive.TANK_TRANSMISSION_DISTRIBUTOR_ASSEMBLED_ITEM,
                                CreateMechanicalDrive.TANK_TRANSMISSION_STEERING_ASSEMBLED_ITEM,
                                CreateMechanicalDrive.TANK_TRANSMISSION_FRAME_ITEM,
                                CreateMechanicalDrive.TANK_TRANSMISSION_HOUSING_ITEM,
                                CreateMechanicalDrive.TANK_TRANSMISSION_DISTRIBUTOR_ITEM,
                                CreateMechanicalDrive.TANK_TRANSMISSION_STEERING_ITEM,
                                CreateMechanicalDrive.ENGINE_ITEM,
                                CreateMechanicalDrive.STIRLING_ENGINE_HEATER_ITEM,
                                CreateMechanicalDrive.STIRLING_ENGINE_HEATER_COVER_ITEM,
                                CreateMechanicalDrive.STIRLING_ENGINE_CORE_ITEM,
                                CreateMechanicalDrive.STIRLING_ENGINE_OUTPUT_ITEM,
                                CreateMechanicalDrive.STIRLING_ENGINE_FLYWHEEL_ITEM,
                                CreateMechanicalDrive.GEARBOX_AXIAL_LEVER_ITEM,
                                CreateMechanicalDrive.GEARBOX_LINEAR_LEVER_ITEM,
                                CreateMechanicalDrive.STEERING_WHEEL_ITEM
                        )
                )
        );

        register(
                new CreativeTabSection(
                        "running_gear",

                        Component.translatable(
                                "creative_section.mechanical_drive.running_gear"
                        ),

                        0xFFFFEB8C,
                        0xAA39231C,

                        120,

                        List.of(
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/running_gear/running_gear_00"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/running_gear/running_gear_01"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/running_gear/running_gear_02"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/running_gear/running_gear_03"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/running_gear/running_gear_04"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/running_gear/running_gear_05"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/running_gear/running_gear_06"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/running_gear/running_gear_07"
                                )
                        ),

                        List.of(
                                CreateMechanicalDrive.SPROCKET_MOUNT_ITEM,
                                CreateMechanicalDrive.SPROCKET_WHEEL_ITEM,
                                CreateMechanicalDrive.TORSION_MOUNT_ITEM,
                                CreateMechanicalDrive.DRIVE_WHEEL_ITEM,
                                CreateMechanicalDrive.LONG_TORSION_MOUNT_ITEM,
                                CreateMechanicalDrive.DRIVE_WHEEL_BIG_ITEM,
                                CreateMechanicalDrive.SUPPORT_WHEEL_ITEM,
                                CreateMechanicalDrive.IDLER_MOUNT_ITEM,
                                CreateMechanicalDrive.IDLER_WHEEL_ITEM,
                                CreateMechanicalDrive.NARROW_TRACK_LINK_ITEM,
                                CreateMechanicalDrive.WIDE_TRACK_LINK_ITEM,
                                CreateMechanicalDrive.STEERING_WHEEL_MOUNT_ITEM,
                                CreateMechanicalDrive.DOUBLE_STEERING_WHEEL_MOUNT_ITEM,
                                CreateMechanicalDrive.DOUBLE_WHEEL_MOUNT_ITEM,
                                CreateMechanicalDrive.RIGID_STEERING_WHEEL_MOUNT_ITEM,
                                CreateMechanicalDrive.DOUBLE_RIGID_STEERING_WHEEL_MOUNT_ITEM,
                                CreateMechanicalDrive.RIGID_WHEEL_MOUNT_ITEM,
                                CreateMechanicalDrive.DOUBLE_RIGID_WHEEL_MOUNT_ITEM,
                                CreateMechanicalDrive.SEPARATED_SMALL_TIRE_ITEM,
                                CreateMechanicalDrive.DOUBLE_SMALL_TIRE_ITEM,
                                CreateMechanicalDrive.SEPARATED_TIRE_ITEM,
                                CreateMechanicalDrive.DOUBLE_TIRE_ITEM,
                                CreateMechanicalDrive.SCREWDRIVER_ITEM,
                                CreateMechanicalDrive.ADJUSTMENT_WRENCH_ITEM,
                                CreateMechanicalDrive.SPRING_TUNING_WRENCH_ITEM
                        )
                )
        );

        register(
                new CreativeTabSection(
                        "components",

                        Component.translatable(
                                "creative_section.mechanical_drive.components"
                        ),

                        0xFFFFEB8C,
                        0xAA39231C,

                        120,

                        List.of(
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/components/components_00"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/components/components_01"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/components/components_02"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/components/components_03"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/components/components_04"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/components/components_05"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/components/components_06"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/components/components_07"
                                )
                        ),

                        List.of(
                                CreateMechanicalDrive.SERVICE_TANK_ITEM,
                                CreateMechanicalDrive.DOG_CLUTCH_ITEM,
                                CreateMechanicalDrive.OVERRUNNING_CLUTCH_ITEM,
                                CreateMechanicalDrive.GEAR_REDUCER_ITEM,
                                CreateMechanicalDrive.SHAFT_DISTRIBUTOR_ITEM,
                                CreateMechanicalDrive.FOUR_WAY_SHAFT_DISTRIBUTOR_ITEM,
                                CreateMechanicalDrive.ROTARY_LIMITER_ITEM,
                                CreateMechanicalDrive.MECHANICAL_STARTER_ITEM,
                                CreateMechanicalDrive.MECHANICAL_JACK_ITEM,
                                CreateMechanicalDrive.HAND_CRANK_ITEM,
                                CreateMechanicalDrive.WORM_GEAR_REGULAR_ITEM,
                                CreateMechanicalDrive.WORM_GEAR_SMALL_ITEM,
                                CreateMechanicalDrive.ANGLE_GEAR_ITEM,
                                CreateMechanicalDrive.SHAFT_MARKER_ITEM,
                                CreateMechanicalDrive.CHAIN_GEAR_ITEM,
                                CreateMechanicalDrive.CHAIN_LINKAGE_ITEM,
                                CreateMechanicalDrive.CARDAN_SHAFT_ITEM,
                                CreateMechanicalDrive.SUSPENSION_STRUT_ITEM,
                                CreateMechanicalDrive.RIGID_LINK_JOINT_ITEM,
                                CreateMechanicalDrive.RIGID_LINK_ITEM,
                                CreateMechanicalDrive.RIGID_LINK_LIMITED_ITEM,
                                CreateMechanicalDrive.SEAT_ITEM,
                                CreateMechanicalDrive.FLAT_SEAT_ITEM
                        )
                )
        );

        register(
                new CreativeTabSection(
                        "crafting_components",

                        Component.translatable(
                                "creative_section.mechanical_drive.crafting_components"
                        ),

                        0xFFFFEB8C,
                        0xAA39231C,

                        80,

                        List.of(
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/crafting_components/crafting_components_00"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/crafting_components/crafting_components_01"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/crafting_components/crafting_components_02"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/crafting_components/crafting_components_03"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/crafting_components/crafting_components_04"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/crafting_components/crafting_components_05"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/crafting_components/crafting_components_06"
                                ),
                                ResourceLocation.fromNamespaceAndPath(
                                        CreateMechanicalDrive.MOD_ID,
                                        "creative/crafting_components/crafting_components_07"
                                )
                        ),

                        List.of(
                                CreateMechanicalDrive.IRON_ROD_ITEM,
                                CreateMechanicalDrive.SHORT_TORSION_BAR_ITEM,
                                CreateMechanicalDrive.TORSION_MOUNT_COMPONENT_ITEM,
                                CreateMechanicalDrive.LONG_TORSION_BAR_ITEM,
                                CreateMechanicalDrive.LONG_TORSION_MOUNT_COMPONENT_ITEM,
                                CreateMechanicalDrive.TANK_WHEEL_BASE_ITEM,
                                CreateMechanicalDrive.TANK_DRIVE_WHEEL_ITEM,
                                CreateMechanicalDrive.TANK_BIG_DRIVE_WHEEL_ITEM,
                                CreateMechanicalDrive.TANK_IDLER_WHEEL_ITEM,
                                CreateMechanicalDrive.TANK_SPROCKET_WHEEL_ITEM,
                                CreateMechanicalDrive.BEARING_BALLS_ITEM,
                                CreateMechanicalDrive.BEARING_ELEMENT_ITEM,
                                CreateMechanicalDrive.LINK_ELEMENT_FREE_ITEM,
                                CreateMechanicalDrive.LINK_ELEMENT_LIMITED_ITEM,
                                CreateMechanicalDrive.CARDAN_YOKE_ELEMENT_ITEM,
                                CreateMechanicalDrive.CARDAN_ELEMENT_ITEM
                        )
                )
        );
    }

    private CreativeTabSections() {
    }

    public static CreativeTabSection register(
            CreativeTabSection section
    ) {
        SECTIONS.add(section);
        return section;
    }

    @SafeVarargs
    public static void registerUnsectioned(
            Supplier<? extends Item>... items
    ) {
        UNSECTIONED_ITEMS.addAll(List.of(items));
    }

    public static List<CreativeTabSection> sections() {
        return List.copyOf(SECTIONS);
    }

    public static Map<String, Integer> bannerRows() {
        return Map.copyOf(BANNER_ROWS);
    }

    public static boolean isMechanicalDriveTab(
            CreativeModeTab tab
    ) {
        return tab == CreateMechanicalDrive.MAIN_TAB.get();
    }

    public static int getCurrentScrollRow() {
        return currentScrollRow;
    }

    public static void setCurrentScrollRow(int row) {
        currentScrollRow = row;
    }

    public static void buildContents(
            List<ItemStack> displayItems,
            Set<ItemStack> searchItems
    ) {
        displayItems.clear();
        searchItems.clear();
        BANNER_ROWS.clear();

        for (CreativeTabSection section : SECTIONS) {

            padToNextRow(displayItems);

            int bannerRow = displayItems.size() / COLUMNS;

            BANNER_ROWS.put(
                    section.id(),
                    bannerRow
            );

            for (int i = 0; i < COLUMNS; i++) {
                displayItems.add(ItemStack.EMPTY);
            }

            for (Supplier<? extends Item> itemSupplier
                    : section.items()) {

                ItemStack stack =
                        itemSupplier.get().getDefaultInstance();

                displayItems.add(stack);
                searchItems.add(stack);
            }

            padToNextRow(displayItems);
        }

        for (Supplier<? extends Item> itemSupplier
                : UNSECTIONED_ITEMS) {

            ItemStack stack =
                    itemSupplier.get().getDefaultInstance();

            displayItems.add(stack);
            searchItems.add(stack);
        }
    }

    private static void padToNextRow(
            List<ItemStack> items
    ) {
        int remainder = items.size() % COLUMNS;

        if (remainder == 0) {
            return;
        }

        int missing = COLUMNS - remainder;

        for (int i = 0; i < missing; i++) {
            items.add(ItemStack.EMPTY);
        }
    }
}
