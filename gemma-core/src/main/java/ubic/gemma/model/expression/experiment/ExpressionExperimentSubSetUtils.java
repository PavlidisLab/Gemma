package ubic.gemma.model.expression.experiment;

import org.apache.commons.lang3.Strings;

/**
 * Utility methods for {@link ExpressionExperimentSubSet}.
 *
 * @author poirigui
 */
public class ExpressionExperimentSubSetUtils {

    /**
     * Obtain the unprefixed name of a subset, i.e., the name without the source experiment name and delimiter.
     */
    public static String getUnprefixedName( ExpressionExperimentSubSet subset ) {
        // these are for subsets created by the LinearModelAnalyzer prior to 1.32.6
        if ( subset.getName().startsWith( "Subset for " ) ) {
            return Strings.CS.removeStart( subset.getName(), "Subset for " );
        }

        return Strings.CS.removeStart( subset.getName(),
                subset.getSourceExperiment().getName() + ExpressionExperimentSubSet.NAME_DELIMITER );
    }
}
