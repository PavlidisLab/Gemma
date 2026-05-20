package ubic.gemma.core.util.math.linearmodels;

import ubic.gemma.core.util.matrix.DoubleMatrix;

import java.util.*;

public class LinearModelSummaryUtils {

    public static Map<String, Collection<String>> createTerm2CoefficientNames( List<String> factorNames, DoubleMatrix<String, String> contrastCoefficients ) {
        Map<String, Collection<String>> term2CoefficientNames = new HashMap<>();
        for ( String string : factorNames ) {
            term2CoefficientNames.put( string, new HashSet<>() );
        }
        List<String> coefRowNames = contrastCoefficients.getRowNames();
        for ( String coefNameFromR : coefRowNames ) {
            if ( coefNameFromR.equals( LinearModelSummary.INTERCEPT_COEFFICIENT_NAME ) ) {
                continue; // ?
            } else if ( coefNameFromR.contains( ":" ) ) {
                /*
                 * We're counting on the interaction terms names ending up like this: f1001fv1005:f1002fv1006 (see
                 * LinearModelAnalyzer in Gemma, which sets up the factor and factorvalue names in the way that
                 * generates this). Risky, and it won't work for continuous factors. But R makes kind of a mess from the
                 * interactions. If we assume there is only one interaction term we can work it out by the presence of
                 * ":".
                 */
                String cleanedInterationTermName = coefNameFromR.replaceAll( "fv_[0-9]+(?=(:|$))", "" );

                for ( String factorName : factorNames ) {
                    if ( !factorName.contains( ":" ) ) continue;

                    if ( factorName.equals( cleanedInterationTermName ) ) {
                        assert term2CoefficientNames.containsKey( factorName );
                        term2CoefficientNames.get( factorName ).add( coefNameFromR );
                    }

                }

            } else {

                for ( String factorName : factorNames ) {
                    if ( coefNameFromR.startsWith( factorName ) ) {
                        assert term2CoefficientNames.containsKey( factorName );
                        term2CoefficientNames.get( factorName ).add( coefNameFromR );

                    }
                }
            }
        }
        return term2CoefficientNames;
    }
}
