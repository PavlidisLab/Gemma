package ubic.gemma.util;

import java.util.Comparator;

import ubic.gemma.model.expression.experiment.FactorValue;

/**
 * @author luke
 */
public class FactorValueComparator implements Comparator<FactorValue> {
    private static FactorValueComparator _instance = new FactorValueComparator();

    public static FactorValueComparator getInstance() {
        return _instance;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
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