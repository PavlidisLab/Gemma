package ubic.gemma.model.common.description;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import ubic.gemma.model.expression.experiment.Statement;

import org.springframework.lang.Nullable;
import java.util.Objects;
import java.util.stream.Stream;

public class CharacteristicUtils {

    /**
     * Normalize a characteristic by value.
     * <p>
     * This is obtained by taking the value URI or value if the former is null and converting it to lowercase.
     */
    @Nullable
    public static String getNormalizedValue( Characteristic characteristic ) {
        if ( characteristic.getValueUri() != null ) {
            return characteristic.getValueUri().toLowerCase();
        } else if ( characteristic.getValue() != null ) {
            return characteristic.getValue().toLowerCase();
        } else {
            return null;
        }
    }

    /**
     * Check if a given characteristics has a specific category.
     * <p>
     * Comparisons are performed as per {@link #equals(String, String, String, String)}.
     */
    public static boolean hasCategory( Characteristic c, Category category ) {
        return equals( c.getCategory(), c.getCategoryUri(), category.getCategory(), category.getCategoryUri() );
    }

    /**
     * Create a new characteristic that represents the category of a given characteristic.
     */
    public static Category getCategory( Characteristic t ) {
        return new Category( t.getCategory(), t.getCategoryUri() );
    }

    public static Characteristic getCategoryAsCharacteristic( Characteristic t ) {
        Characteristic c = new Characteristic();
        c.setCategory( t.getCategory() );
        c.setCategoryUri( t.getCategoryUri() );
        return c;
    }

    /**
     * Check if the given characteristic has a particular value.
     */
    public static boolean hasValue( Characteristic c, Value value ) {
        return equals( c.getValue(), c.getValueUri(), value.getValue(), value.getValueUri() );
    }

    /**
     * Check if the given characteristic has any of the specified values.
     */
    public static boolean hasAnyValue( Characteristic c, Value... values ) {
        return Stream.of( values ).anyMatch( v -> hasValue( c, v ) );
    }

    /**
     * Check if the given characteristic is uncategorized.
     */
    public static boolean isUncategorized( Characteristic c ) {
        return c.getCategory() == null && c.getCategoryUri() == null;
    }

    /**
     * Check if the given characteristic has or is a free-text category.
     */
    public static boolean isFreeTextCategory( Characteristic c ) {
        return c.getCategory() != null && c.getCategoryUri() == null;
    }

    /**
     * Check if the given characteristic is a free-text value.
     */
    public static boolean isFreeText( Characteristic c ) {
        return c.getValue() != null && c.getValueUri() == null;
    }

    /**
     * Hash an ontology term.
     */
    public static int hash( String value, String valueUri ) {
        return Objects.hash( StringUtils.lowerCase( valueUri != null ? valueUri : value ) );
    }

    /**
     * Statement-aware equality for the "is this the same tag?" question driving the idempotent
     * set-replace annotation writes (experiment- and biomaterial-level).
     * <p>
     * A {@link Statement} and a plain {@link Characteristic} with the same (category, value) are NOT
     * the same tag — the Statement carries subject/predicate/object semantics the plain Characteristic
     * lacks, so a wire-shape change (plain &harr; Statement) must round-trip as a drop+add, not a no-op.
     * Two Statements additionally match on their predicate/object and second predicate/object pairs.
     * Comparisons delegate to {@link #equals(String, String, String, String)} (case-insensitive,
     * URI-aware). Used by both {@code ExpressionExperimentService.updateAnnotations} and
     * {@code BioMaterialService.updateAnnotations} so the two diff implementations cannot drift.
     */
    public static boolean sameTag( Characteristic a, Characteristic b ) {
        boolean aIsStatement = a instanceof Statement;
        boolean bIsStatement = b instanceof Statement;
        if ( aIsStatement != bIsStatement ) {
            return false;
        }
        boolean baseEqual = equals( a.getCategory(), a.getCategoryUri(), b.getCategory(), b.getCategoryUri() )
                && equals( a.getValue(), a.getValueUri(), b.getValue(), b.getValueUri() );
        if ( !baseEqual ) {
            return false;
        }
        if ( !aIsStatement ) {
            return true;
        }
        Statement sa = ( Statement ) a;
        Statement sb = ( Statement ) b;
        return equals( sa.getPredicate(), sa.getPredicateUri(), sb.getPredicate(), sb.getPredicateUri() )
                && equals( sa.getObject(), sa.getObjectUri(), sb.getObject(), sb.getObjectUri() )
                && equals( sa.getSecondPredicate(), sa.getSecondPredicateUri(), sb.getSecondPredicate(), sb.getSecondPredicateUri() )
                && equals( sa.getSecondObject(), sa.getSecondObjectUri(), sb.getSecondObject(), sb.getSecondObjectUri() );
    }

    /**
     * Parse a characteristic's opaque {@code supportingEvidence} JSON into a tree for serialization.
     * <p>
     * The column is a verbatim provenance payload the curation agents emitted (the agents-side
     * {@code FindingEvidence} shape: a JSON array of {@code {quote, source, location, …}} items). Gemma stores
     * and serves it opaquely — the agents repo owns the schema — so this only turns the stored string back into
     * a tree. Writes always store a serialized tree, so it round-trips; a null / blank or (defensively)
     * unparseable value yields {@code null} rather than propagating a parse failure into a read response.
     * <p>
     * Lives here rather than on any one value object because every read surface over a
     * {@link Characteristic} needs the same treatment — {@link AnnotationValueObject},
     * {@link CharacteristicValueObject}, and the design path's
     * {@code StatementValueObject} — and three private copies would be three chances to drift.
     */
    @Nullable
    public static JsonNode parseSupportingEvidence( @Nullable String json ) {
        if ( json == null || json.isEmpty() ) {
            return null;
        }
        try {
            return SUPPORTING_EVIDENCE_MAPPER.readTree( json );
        } catch ( Exception e ) {
            return null;
        }
    }

    /**
     * Inverse of {@link #parseSupportingEvidence}: flatten a supporting-evidence tree back to the string the
     * {@code SUPPORTING_EVIDENCE} column stores.
     * <p>
     * An absent, null, or empty tree yields {@code null} rather than {@code "[]"} or {@code "null"}, so
     * "nothing recorded" has exactly one representation in the database and a caller cannot accidentally
     * persist an empty array that later reads as though evidence were recorded and found wanting.
     */
    @Nullable
    public static String serializeSupportingEvidence( @Nullable JsonNode evidence ) {
        if ( evidence == null || evidence.isNull() || evidence.isEmpty() ) {
            return null;
        }
        return evidence.toString();
    }

    private static final ObjectMapper SUPPORTING_EVIDENCE_MAPPER = new ObjectMapper();

    /**
     * Compare a pair of ontology terms.
     */
    public static boolean equals( String a, String aUri, String b, String bUri ) {
        if ( aUri != null ^ bUri != null ) {
            return false; // free-text v.s. ontology term, always false
        }
        return aUri != null ? Strings.CI.equals( aUri, bUri ) : Strings.CI.equals( a, b );
    }

    /**
     * Compare a pair of ontology terms.
     * <p>
     * Terms are sorted by label and then URI. If two term have an identical URI, this method will return zero
     * regardless of the label.
     * <p>
     * All URI and label comparisons are case-insensitive.
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
