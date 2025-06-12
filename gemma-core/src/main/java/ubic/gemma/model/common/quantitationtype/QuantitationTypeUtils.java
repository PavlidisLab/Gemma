package ubic.gemma.model.common.quantitationtype;

import org.apache.commons.lang3.StringUtils;

/**
 * Utilities for working with {@link QuantitationType}s.
 * @author poirigui
 */
public class QuantitationTypeUtils {

    /**
     * Check if a given quantitation type holds log2 CPM.
     */
    public static boolean isLog2cpm( QuantitationType qt ) {
        return StringUtils.containsIgnoreCase( qt.getName(), "log2cpm" ) &&
                qt.getGeneralType() == GeneralType.QUANTITATIVE &&
                qt.getType() == StandardQuantitationType.AMOUNT &&
                qt.getScale() == ScaleType.LOG2;
    }

    /**
     * Check if a given QT holds log-transformed data.
     */
    public static boolean isLogTransformed( QuantitationType qt ) {
        return qt.getScale() == ScaleType.LOG2 || qt.getScale() == ScaleType.LN || qt.getScale() == ScaleType.LOG10
                || qt.getScale() == ScaleType.LOG1P || qt.getScale() == ScaleType.LOGBASEUNKNOWN;
    }

    /**
     * Check if a given quantitation type contains counting data.
     * <p>
     * Note that counting data might not necessarily use the {@link ScaleType#COUNT} scale.
     */
    public static boolean isCount( QuantitationType qt ) {
        return qt.getGeneralType() == GeneralType.QUANTITATIVE && qt.getType() == StandardQuantitationType.COUNT;
    }
}
