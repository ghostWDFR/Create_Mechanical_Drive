# Third-party asset notice

Create Mechanical Drive does not bundle texture PNG files copied from Create,
Create Aeronautics, Simulated, Offroad, or Sable.

Some JSON models and runtime render paths contain references to external
resource locations. These references are resolved by Minecraft from the user's
installed mods at runtime. The referenced third-party files are not redistributed
inside this mod's `assets/mechanical_drive` namespace unless explicitly stated
below.

Create Mechanical Drive includes a modified model derived from the wheel mount
model used by Offroad, which is distributed as part of the Create Aeronautics /
Simulated ecosystem.

Permission to use and modify this model in Create Mechanical Drive was granted
directly by the developer of Offroad.

Textures used by this modified model are not bundled into Create Mechanical
Drive. They continue to reference the corresponding Offroad resource locations
at runtime, so Offroad remains the source of those texture assets.

External Create resource locations currently referenced for model or texture
resolution include:

- `create:block/andesite_casing`
- `create:block/andesite_encased_cogwheel_side`
- `create:block/axis`
- `create:block/axis_top`
- `create:block/cogwheel`
- `create:block/cogwheel_axis`
- `create:block/cogwheel_shaftless`
- `create:block/gearbox`
- `create:block/gearshift_off`
- `create:block/gearshift_on`
- `create:block/large_wheels`
- `create:block/shaft`

Create Mechanical Drive may also reference models, textures, registries, item
ids, recipe types, APIs, or other runtime resources provided by dependency mods
such as Create, Create Aeronautics, Simulated, Offroad, and Sable.

References to external resource locations do not imply redistribution of the
referenced files. Unless explicitly included under permission, those resources
remain provided by their respective dependency mods.

Vanilla `minecraft:*` resource locations may appear where Minecraft's own
runtime resources or render metadata are used.