package ubic.gemma.model.expression.experiment;

import org.apache.commons.lang3.StringUtils;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

/**
 * @author poirigui
 */
public class StatementUtils {

    /**
     * List of predicates whose object should appear before the subject.
     * <p>
     * For example. "headache has phenotype acute" should be "acute headache".
     */
    private static final String[] PREDICATE_THAT_SHOULD_BE_REVERSED = new String[] {
            "http://purl.obolibrary.org/obo/RO_0002200",   // has phenotype
            "http://purl.obolibrary.org/obo/GENO_0000222", // has genotype
            "http://purl.obolibrary.org/obo/RO_0000053",   // has characteristic
            "http://gemma.msl.ubc.ca/ont/TGEMO_00168",     // has development stage
            // "http://purl.obolibrary.org/obo/RO_0002573"    // has modifier
    };

    /**
     * Format a statement.
     */
    public static String formatStatement( Statement statement ) {
        return formatStatement( statement, new String[0] );
    }

    /**
     * Format a statement.
     * @param statement            statement to format.
     * @param ignoredPredicateUris list of predicate URIs that shouldn't be included in the formatted statement
     */
    public static String formatStatement( Statement statement, String[] ignoredPredicateUris ) {
        // special case: all predicate that should be reversed, so we combine all the objects before the subject
        // this also covers the case where the is only a subject since allMatch is true when the stream is empty
        boolean shouldAllBeReversed = IntStream.range( 0, statement.getNumberOfStatements() )
                .filter( i -> hasPredicateAndObject( statement, i, ignoredPredicateUris ) )
                .allMatch( i -> shouldBeReversed( statement, i ) );
        if ( shouldAllBeReversed ) {
            String prefix = IntStream.range( 0, statement.getNumberOfStatements() )
                    .filter( i -> hasPredicateAndObject( statement, i, ignoredPredicateUris ) )
                    .mapToObj( i -> formatObjectAndPredicate( statement, i ) )
                    .collect( Collectors.joining( " " ) );
            if ( prefix.isEmpty() ) {
                return formatObject( statement.getSubject(), statement.getSubjectUri() );
            } else {
                return prefix + " " + formatObject( statement.getSubject(), statement.getSubjectUri() );
            }
        }

        StringBuilder buf = new StringBuilder();
        boolean subjectWasAdded = false;
        for ( int i = 0; i < statement.getNumberOfStatements(); i++ ) {
            if ( !hasPredicateAndObject( statement, i, ignoredPredicateUris ) ) {
                continue;
            }
            if ( subjectWasAdded ) {
                buf.append( " and " );
            }
            if ( shouldBeReversed( statement, i ) ) {
                buf.append( formatObjectAndPredicate( statement, i ) );
                // only add the subject if it wasn't already added
                if ( !subjectWasAdded ) {
                    buf.append( " " ).append( formatObject( statement.getSubject( i ), statement.getSubjectUri( i ) ) );
                }
            } else {
                if ( !subjectWasAdded ) {
                    buf.append( formatObject( statement.getSubject( i ), statement.getSubjectUri( i ) ) ).append( " " );
                }
                buf.append( formatPredicateAndObject( statement, i ) );
            }
            subjectWasAdded = true;
        }

        return buf.toString();
    }

    private static boolean shouldBeReversed( Statement statement, int index ) {
        return StringUtils.equalsAnyIgnoreCase( statement.getPredicateUri( index ), PREDICATE_THAT_SHOULD_BE_REVERSED );
    }

    /**
     * Check if a statement as a predicate and an object for a given index.
     */
    private static boolean hasPredicateAndObject( Statement statement, int index, String[] ignoredPredicateUris ) {
        return statement.getPredicate( index ) != null && statement.getObject( index ) != null && !StringUtils.equalsAnyIgnoreCase( statement.getPredicateUri( index ), ignoredPredicateUris );
    }

    /**
     * Format the object and predicate (in reverse order) at a given index.
     */
    private static String formatObjectAndPredicate( Statement c, int index ) {
        return formatObjectAndPredicate( c.getPredicate( index ), c.getPredicateUri( index ), c.getObject( index ), c.getObjectUri( index ) );
    }

    private static String formatObjectAndPredicate( String predicate, String predicateUri, String object, String objectUri ) {
        String suffix;
        if ( StringUtils.equalsAnyIgnoreCase( objectUri,
                "http://gemma.msl.ubc.ca/ont/TGEMO_00004", // overexpression
                "http://gemma.msl.ubc.ca/ont/TGEMO_00007", // knockdown
                "http://purl.obolibrary.org/obo/PATO_0001997", // decreased amount
                "http://purl.obolibrary.org/obo/PATO_0000470" // increased amount
        ) ) {
            suffix = " of ";
        } else {
            suffix = " ";
        }
        return formatObject( object, objectUri ) + suffix;
    }

    /**
     * Format the object and predicate at a given index.
     */
    private static String formatPredicateAndObject( Statement c, int index ) {
        return formatPredicateAndObject( c.getPredicate( index ), c.getPredicateUri( index ), c.getObject( index ), c.getObjectUri( index ) );
    }

    private static String formatPredicateAndObject( String predicate, String predicateUri, String object, String objectUri ) {
        return formatPredicate( predicate, predicateUri ) + " " + formatObject( object, objectUri );
    }

    private static String formatPredicate( String predicate, String predicateUri ) {
        if ( "http://purl.obolibrary.org/obo/GENO_0000222".equalsIgnoreCase( predicateUri ) ) {
            // the label in GENO has an underscore
            return "has genotype";
        } else if ( "http://purl.obolibrary.org/obo/GENO_0000413".equalsIgnoreCase( predicateUri ) ) {
            return "has allele";
        } else if ( "http://purl.obolibrary.org/obo/RO_0001000".equalsIgnoreCase( predicateUri ) ) {
            return "derived from";
        } else if ( "http://purl.obolibrary.org/obo/ENVO_01003004".equalsIgnoreCase( predicateUri ) ) {
            return "derived from part of";
        } else if ( "http://purl.obolibrary.org/obo/CLO_0000015".equalsIgnoreCase( predicateUri ) ) {
            return "derived from patient with";
        } else if ( "http://purl.obolibrary.org/obo/RO_0002573".equalsIgnoreCase( predicateUri ) ) {
            return "with";
        }
        return defaultIfBlank( predicate, "?" );
    }

    private static String formatObject( String object, String objectUri ) {
        if ( "http://purl.obolibrary.org/obo/SO_0002054".equalsIgnoreCase( objectUri ) ) {
            // loss_of_function_variant
            return "loss of";
        } else if ( "http://purl.obolibrary.org/obo/SO_0002315".equalsIgnoreCase( objectUri ) ) {
            return "overexpression of";
        } else {
            // increased_gene_product_level
            return defaultIfBlank( object, "?" );
        }
    }
}
