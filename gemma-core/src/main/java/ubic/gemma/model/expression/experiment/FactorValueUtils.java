package ubic.gemma.model.expression.experiment;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.measurement.Measurement;

import java.util.Iterator;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

public class FactorValueUtils {

    /**
     * Produce a summary string for this factor value.
     */
    public static String getSummaryString( FactorValue fv ) {
        return getSummaryString( fv, ", " );
    }

    public static String getSummaryString( FactorValue fv, String statementDelimiter ) {
        StringBuilder buf = new StringBuilder();
        if ( fv.getMeasurement() != null ) {
            if ( fv.getExperimentalFactor() != null && fv.getExperimentalFactor().getCategory() != null ) {
                buf.append( defaultIfBlank( fv.getExperimentalFactor().getCategory().getCategory(), "?" ) )
                        .append( ": " );
            }
            Measurement measurement = fv.getMeasurement();
            buf.append( defaultIfBlank( measurement.getValue(), "?" ) );
            if ( fv.getMeasurement().getUnit() != null ) {
                buf.append( " " ).append( fv.getMeasurement().getUnit().getUnitNameCV() );
            }
        } else if ( !fv.getCharacteristics().isEmpty() ) {
            for ( Iterator<Statement> iter = fv.getCharacteristics().iterator(); iter.hasNext(); ) {
                Statement c = iter.next();
                buf.append( StatementUtils.formatStatement( c ) );
                if ( iter.hasNext() )
                    buf.append( statementDelimiter );
            }
        } else if ( StringUtils.isNotBlank( fv.getValue() ) ) {
            buf.append( fv.getValue() );
        } else {
            buf.append( "?" );
        }
        return buf.toString();
    }
}
