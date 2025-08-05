package ubic.gemma.model.expression.experiment;

import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.common.measurement.Measurement;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

public class FactorValueUtils {

    /**
     * Produce a value for representing a factor value.
     * <p>
     * For continuous factors, this will return the value of the measurement. For categorical factors, this will be the
     * subject (or |-delimited concatenation of subjects) of the statements. If there are no statements,
     * {@link FactorValue#getValue()} will be used as a fallback.
     * <p>
     * This value is suitable for displaying in a flat file format. If you need a human-readable summary of the factor
     * value, consider using {@link #getSummaryString(FactorValue)} instead.
     * @param delimiter delimiter used when joining multiple values from statements
     */
    @Nullable
    public static String getValue( FactorValue fv, String delimiter ) {
        if ( fv.getMeasurement() != null ) {
            return fv.getMeasurement().getValue();
        } else {
            String valueFromStatements = fv.getCharacteristics().stream()
                    .map( Statement::getSubject )
                    .sorted()
                    .collect( Collectors.joining( delimiter ) );
            if ( StringUtils.isNotBlank( valueFromStatements ) ) {
                return valueFromStatements;
            } else if ( StringUtils.isNotBlank( fv.getValue() ) ) {
                return fv.getValue();
            } else {
                return null;
            }
        }
    }

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
            for ( Iterator<Statement> iter = fv.getCharacteristics().stream().sorted().iterator(); iter.hasNext(); ) {
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
