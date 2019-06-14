/**
 * Copyright (c) 2017 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.utils;

import java.io.IOException;
import java.io.InputStream;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.controlsfx.glyphfont.GlyphFont;
import org.controlsfx.glyphfont.GlyphFontRegistry;

/**
 * Factory class for glyphs from Fontawesome library. <br>
 * <b>Usage example:</b>
 *
 * <pre>
 * Glyph g = GlyphFactory.create(FontAwesome.Glyph.CLOSE);
 * g.setTextFill(Color.RED);
 * Button button = new Button("", g);
 * </pre>
 *
 * @author Luca Molinari
 */
public final class GlyphFactory { // NOPMD nomen est fix
    private static GlyphFont fontAwesome;

    private GlyphFactory() {
    }

    static {
        try (InputStream is = GlyphFactory.class.getResourceAsStream("FONT_AWESOME-webfont")) {
            GlyphFactory.fontAwesome = new FontAwesome(is);
            GlyphFontRegistry.register(GlyphFactory.fontAwesome);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a glyph for given identifier
     *
     * @param icon one of the font code available in {@link FontAwesome} Glyph enum.
     * @return created glyph
     */
    public static synchronized Glyph create(final FontAwesome.Glyph icon) {
        if (GlyphFactory.fontAwesome == null) {
            try (InputStream is = GlyphFactory.class.getResourceAsStream("FONT_AWESOME-webfont")) {
                GlyphFactory.fontAwesome = new FontAwesome(is);
                GlyphFontRegistry.register(GlyphFactory.fontAwesome);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        return GlyphFactory.fontAwesome.create(icon);
    }

}
