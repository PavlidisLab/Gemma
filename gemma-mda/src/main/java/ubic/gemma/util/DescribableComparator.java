package ubic.gemma.util;

import java.util.Comparator;

import ubic.gemma.model.common.Describable;

/**
 * @author luke
 *
 */
public class DescribableComparator implements Comparator<Describable> {
    private static DescribableComparator _instance = new DescribableComparator();
    
    public static DescribableComparator getInstance() { return _instance; }
    
    
    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( Describable d1, Describable d2 ) {
        String s1 = d1.getName();
        String s2 = d2.getName();
        if (s1 != null) {
            if (s2 != null)
                    return s1.compareTo( s2 );
            else
                return 1;
        } else {
            if (s2 != null)
                return -1;
            else
                return 0;
        }
    }
}
