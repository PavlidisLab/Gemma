/**
 * 
 */
package ubic.gemma.web.taglib.displaytag;

import java.util.Comparator;

/**
 * @author jsantos
 *
 * Comparator for strings in displayTag. This ignores case.
 */
public class StringComparator implements Comparator {

    /* (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( Object o1, Object o2 ) {
        String s1 = (String) o1;
        String s2 = (String) o2;
        
        return s1.compareToIgnoreCase( s2 );
    }

}
