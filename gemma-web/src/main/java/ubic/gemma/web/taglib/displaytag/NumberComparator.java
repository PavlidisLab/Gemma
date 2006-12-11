/**
 * 
 */
package ubic.gemma.web.taglib.displaytag;

import java.util.Comparator;

/**
 * @author jsantos 
 * 
 * Comparator for simple numbers in displayTag. If the string is not just a number (ie the cell has a
 *         link as well), write a customized comparator.
 */
public class NumberComparator implements Comparator {

    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( Object arg0, Object arg1 ) {
        String s1 = "0";
        String s2 = "0";
        double num1;
        double num2;

        // do some reflection to determine if the numbers need to be parsed from String
        // if it is not an integer, assume it is a number of some sort. Convert to string.
        try {
            if (arg0.getClass() == Class.forName( "java.lang.String" ) && arg1.getClass() == Class.forName( "java.lang.String" ))  {
                s1 = (String) arg0;
                s2 = (String) arg1;
            }
            else {
                Class c1 = arg0.getClass();
                Class c2 = arg1.getClass();
                
                s1 = c1.cast( arg0 ).toString();
                s2 = c2.cast( arg1 ).toString();
            }
        } catch ( ClassNotFoundException e ) {
            // failsafe, do not sort
            return 0;
        }

        
        
        num1 = Double.parseDouble( ( String ) s1 );
        num2 = Double.parseDouble( ( String ) s2 );
        if ( num1 < num2 ) {
            return -1;
        } else if ( num1 > num2 ) {
            return 1;
        } else {
            return 0;
        }


    }

}
