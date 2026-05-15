# MirageVisuals

Visual client for **Minecraft 1.21.4 Fabric** by **opsetter**.

This is the initial scaffold of the mod plus the first two GUI shaders:

* `rounded_rect` — anti-aliased rounded-rectangle SDF shader, used by
  `RoundedRectRenderer` to draw any rectangle with smooth rounded corners.
* `glass_panel` — pastel-lavender frosted-glass panel with a soft outer
  halo, vertical gradient and antialiased edge. Driven by
  `GlassPanelRenderer`.

The `PressableWidgetMixin` redraws every `PressableWidget` shown on the
vanilla **Title Screen** with a lavender glass body and dark-navy label.
All other screens keep the vanilla button style.

## Building

```
./gradlew build
```

JDK 21 is required (matches Minecraft 1.21.4). The built jar will be at
`build/libs/miragevisuals-<version>.jar`.

## Project layout

```
src/main/java/com/miragevisuals/client/
  MirageVisualsClient.java       # ClientModInitializer entry
  render/
    ShaderCompiler.java          # GLSL load + compile helper
    GlState.java                 # save/restore GL state around raw passes
    RoundedRectRenderer.java     # rounded_rect shader runtime
    GlassPanelRenderer.java      # glass_panel shader runtime
  mixin/
    PressableWidgetMixin.java    # restyles main-menu buttons
  util/
    MirageColors.java            # lavender palette
src/main/resources/assets/miragevisuals/shaders/
  rounded_rect.vsh / .fsh
  glass_panel.vsh  / .fsh
```
