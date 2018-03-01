package ubic.gemma.persistence.util;

import ubic.gemma.model.expression.experiment.FactorValue;

import java.util.Comparator;

/**
 * @author luke
 */
public class FactorValueComparator implements Comparator<FactorValue> {
    private static final FactorValueComparator _instance = new FactorValueComparator();

    public static FactorValueComparator getInstance() {
        return FactorValueComparator._instance;
    }

    @Override
    public int compare( FactorValue v1, FactorValue v2 ) {
        if ( v1 == null && v2 == null ) {
            return 0;
        } else if ( v1 == null ) {
            return -1;
        } else if ( v2 == null ) {
            return 1;
        }
        String s1 = v1.toString();
        String s2 = v2.toString();
        return s1.compareTo( s2 );
    }
}