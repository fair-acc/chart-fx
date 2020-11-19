package de.gsi.chart.renderer.spi.financial.css;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Default Financial Color Schemes implemented by Chart library.
 * The color schemes IDs are String values not enum.
 * User API extension: Create your scheme class with new color schemes and implement or
 * inherit interface FinancialColorSchemeAware, FinancialColorSchemeConfig.
 *
 * @see FinancialColorSchemeAware whole extension if your injected configuration service.
 * @see FinancialColorSchemeConfig possibility to inherit your configuration extension.
 */
public class FinancialColorSchemeConstants {

    public static final String OLDSCHOOL = "OLDSCHOOL";

    public static final String CLEARLOOK = "CLEARLOOK";

    public static final String SAND = "SAND";

    public static final String BLACKBERRY = "BLACKBERRY";

    public static final String DARK = "DARK";

    //--------------------------------------------------------

    /**
     * @return default color schemes information
     */
    public static String[] getDefaultColorSchemes() {
        Field[] declaredFields = FinancialColorSchemeConstants.class.getDeclaredFields();
        List<String> staticFields = new ArrayList<>();
        for (Field field : declaredFields) {
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                staticFields.add(field.getName());
            }
        }
        return staticFields.toArray(new String[0]);
    }

    private FinancialColorSchemeConstants() {}
}
