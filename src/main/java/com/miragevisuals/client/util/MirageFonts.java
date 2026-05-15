package com.miragevisuals.client.util;

import net.minecraft.util.Identifier;

/**
 * Identifiers for the custom fonts MirageVisuals bundles in resources.
 *
 * <p>{@link #KRONA} corresponds to {@code assets/miragevisuals/font/krona.json},
 * which loads {@code krona_one.ttf} from the same namespace.</p>
 */
public final class MirageFonts {
    public static final Identifier KRONA = Identifier.of("miragevisuals", "krona");

    private MirageFonts() {
    }
}
