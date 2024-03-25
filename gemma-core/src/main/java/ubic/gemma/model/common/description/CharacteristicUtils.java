package ubic.gemma.model.common.description;

import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;

public class CharacteristicUtils {

    /**
     * Create a new characteristic that represents the category of a given characteristic.
     */
    public static Characteristic getCategory( Characteristic t ) {
        Characteristic c = new Characteristic();
        c.setCategory( t.getCategory() );
        c.setCategoryUri( t.getCategoryUri() );
        return c;
    }

    /**
     * Compare a pair of ontology terms.
     */
    public static boolean equals( String a, String aUri, String b, String bUri ) {
        if ( aUri != null ^ bUri != null ) {
            return false; // free-text v.s. ontology term, always false
        }
        return aUri != null ? StringUtils.equalsIgnoreCase( aUri, bUri ) : StringUtils.equalsIgnoreCase( a, b );
    }

    /**
     * Compare a pair of ontology terms.
     * <p>
     * Terms are sorted by label and then URI. If two term have an identical URI, this method will return zero
     * regardless of the label.
     * <p>
     *  All URI and label comparisons are case-insensitive.
     */
    public static int compareTerm( String a, @Nullable String aUri, String b, @Nullable String bUri ) {
        if ( aUri != null && bUri != null ) {
            int uriCmp = aUri.compareToIgnoreCase( bUri );
            if ( uriCmp == 0 ) {
                return 0; // same URI, collapse the two terms
            } else {
                return compareLabel( a, b, uriCmp );
            }
        } else if ( aUri != null ) {
            return compareLabel( a, b, -1 );
        } else if ( bUri != null ) {
            return compareLabel( a, b, 1 );
        } else if ( a != null && b != null ) {
            return a.compareToIgnoreCase( b );
        } else if ( a != null ) {
            return -1;
        } else if ( b != null ) {
            return 1;
        } else {
            return 0;
        }
    }

    private static int compareLabel( String a, String b, int uriCmp ) {
        if ( a != null && b != null ) {
            // different URIs with labels, compare labels
            // if labels are identical, we don't want to collapse the terms, so fallback on the URI
            int labelCmp = a.compareToIgnoreCase( b );
            return labelCmp != 0 ? labelCmp : uriCmp;
        } else if ( a != null ) {
            return -1;
        } else if ( b != null ) {
            return 1;
        } else {
            // a and b are null, fallback to comparing URIs
            return uriCmp;
        }
    }
}
