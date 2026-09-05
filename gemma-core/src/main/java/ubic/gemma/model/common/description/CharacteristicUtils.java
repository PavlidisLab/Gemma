package ubic.gemma.model.common.description;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import ubic.gemma.model.expression.experiment.Statement;

import org.springframework.lang.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public class CharacteristicUtils {

    /**
     * Normalize a characteristic by value.
     * <p>
     * This is obtained by taking the value URI or value if the former is null and converting it to lowercase.
     */
    @Nullable
    /**
     * Classpath resource holding every URI Gemma resolves to a different one on read.
     * <p>
     * The same file generates {@code scripts/sql/term_uri_migration.sql}, so the shim and the
     * migration cannot disagree about what the corpus contains.
     */
    private static final String MIGRATION_RESOURCE = "/ubic/gemma/core/ontology/TermUriMigration.tsv";

    /**
     * from-URI &rarr; [to-URI, to-label, rule, lane]. Empty if the resource is missing or unreadable.
     * <p>
     * Slots 0 and 1 are what the shim rewrites; slots 2 and 3 are why, and exist because an outside
     * resolver consuming this table through {@code GET /annotations/canonicalUris} has to be able to
     * tell an ontology-decided row from one decided by how often our curators typed a spelling.
     */
    private static final Map<String, String[]> URI_MIGRATION = loadUriMigration();

    private static Map<String, String[]> loadUriMigration() {
        Map<String, String[]> m = new HashMap<>();
        try ( InputStream in = CharacteristicUtils.class.getResourceAsStream( MIGRATION_RESOURCE ) ) {
            if ( in == null ) {
                return Collections.emptyMap();
            }
            try ( BufferedReader r = new BufferedReader( new InputStreamReader( in, StandardCharsets.UTF_8 ) ) ) {
                String line;
                boolean header = true;
                while ( ( line = r.readLine() ) != null ) {
                    if ( line.isEmpty() || line.charAt( 0 ) == '#' ) {
                        continue;
                    }
                    String[] f = line.split( "\t" );
                    if ( header ) {
                        header = false;
                        if ( f.length > 1 && "from_uri".equals( f[1] ) ) {
                            continue;   // the column header, not a mapping
                        }
                    }
                    // lane, from_uri, from_label, to_uri, to_label, n_annotations, rule
                    if ( f.length >= 5 && !f[1].isEmpty() && !f[3].isEmpty() ) {
                        m.put( f[1], new String[] { f[3], f[4],
                                f.length > 6 ? f[6] : "", f[0] } );
                    }
                }
            }
        } catch ( IOException e ) {
            // A missing or corrupt shim must not take the application down: without it Gemma
            // returns the un-canonicalized term, which is what it returned yesterday.
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap( m );
    }

    /**
     * The URI Gemma should report for a term, which is not always the one stored.
     * <p>
     * Two populations are remapped, and neither is a judgement call made here &mdash; both were
     * settled with evidence and written down in {@code TermUriMigration.tsv}:
     * <ul>
     *   <li><b>malformed URIs</b> &mdash; a bare CURIE ({@code CL:0000236}), a colon where OBO
     *       uses an underscore, an id concatenated with itself. Each repair was verified by
     *       resolving the repaired IRI against the live ontology.</li>
     *   <li><b>CLO twins</b> &mdash; two live CLO classes for one cell line, decided by an EFO
     *       inbound xref, else a definition, else usage.</li>
     * </ul>
     * <p>
     * 🛑 This is a READ-TIME SHIM standing in for a database migration that is written and
     * parked ({@code scripts/sql/term_uri_migration.sql}). It exists because the agent pipeline
     * is calibrated against a May snapshot and migrating prod now would desynchronize them.
     * When the migration runs, empty the resource &mdash; a shim left over a corrected corpus
     * silently rewrites rows that are already right.
     *
     * @return the canonical URI, or {@code uri} unchanged when nothing maps it (the common case)
     */
    @Nullable
    public static String canonicalUri( @Nullable String uri ) {
        if ( uri == null ) {
            return null;
        }
        String[] to = URI_MIGRATION.get( uri );
        return to != null ? to[0] : uri;
    }

    /**
     * The label that goes with {@link #canonicalUri(String)}.
     * <p>
     * The label has to move with the URI. Reporting the new URI beside the old label produces a
     * row that says one thing and means another, and the label is what search matches and what
     * every table renders.
     *
     * @return the canonical label when {@code uri} is remapped, otherwise {@code label} unchanged
     */
    @Nullable
    public static String canonicalLabel( @Nullable String uri, @Nullable String label ) {
        if ( uri == null ) {
            return label;
        }
        String[] to = URI_MIGRATION.get( uri );
        return to != null ? to[1] : label;
    }

    /** @return true if this URI is one the shim rewrites. */
    public static boolean isRemappedUri( @Nullable String uri ) {
        return uri != null && URI_MIGRATION.containsKey( uri );
    }

    /**
     * The whole canonicalization table, from-URI &rarr; [to-URI, to-label, rule, lane].
     * <p>
     * Exposed so a client that resolves terms <em>before</em> asking Gemma can hold the same answer
     * rather than a hand-copied subset: a local synonym table cannot be corrected by a server-side
     * change, which is how two authorities on the same question come to disagree.
     */
    public static Map<String, String[]> getUriMigrations() {
        return URI_MIGRATION;
    }

    /** @return how many mappings the shim carries; 0 once the migration has run. */
    public static int remappedUriCount() {
        return URI_MIGRATION.size();
    }

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
     * Return {@code c} as a {@link Statement}, converting a plain {@link Characteristic} if needed.
     * <p>
     * Experiment-level tags are statements — a bare one is simply a statement with no predicate or
     * object, which is byte-identical in storage to a plain characteristic apart from the discriminator.
     * Normalizing on the way in means an existing tag and a newly written one always compare on content
     * alone, and adding a predicate to a tag later is an update rather than a delete plus recreate.
     * <p>
     * 🛑 Do NOT substitute {@code Statement.Factory.newInstance( Characteristic )}: it copies only
     * category and value, so it would silently drop the evidence code, the supporting evidence and the
     * original value. Every field {@link Characteristic} declares is carried here.
     *
     * @return {@code c} itself when it is already a Statement, so an entity that is already persistent
     *         keeps its identity and is never replaced by a copy.
     */
    public static Statement asStatement( Characteristic c ) {
        if ( c instanceof Statement ) {
            return ( Statement ) c;
        }
        Statement s = Statement.Factory.newInstance();
        s.setCategory( c.getCategory() );
        s.setCategoryUri( c.getCategoryUri() );
        s.setValue( c.getValue() );
        s.setValueUri( c.getValueUri() );
        s.setEvidenceCode( c.getEvidenceCode() );
        s.setOriginalValue( c.getOriginalValue() );
        s.setSupportingEvidence( c.getSupportingEvidence() );
        s.setDescription( c.getDescription() );
        s.setName( c.getName() );
        return s;
    }

    /**
     * Statement-aware equality for the "is this the same tag?" question driving the idempotent
     * set-replace annotation writes (experiment- and biomaterial-level).
     * <p>
     * Identity is the CONTENT — (category, value) plus the two predicate/object pairs — and never the
     * Java type. A subject-only {@link Statement} and a plain {@link Characteristic} with the same
     * (category, value) ARE the same tag: they are byte-identical in storage apart from the
     * discriminator, so calling them different would mean an annotation that nobody edited compares as
     * changed.
     * <p>
     * 🛑 This used to return false whenever one side was a Statement and the other was not, so that a
     * plain &harr; Statement change round-tripped as drop+add. That rule cannot survive experiment tags
     * being upgraded to statements: during the upgrade, one side of every comparison is whichever form
     * the row or the caller happens to carry. Under the old rule an identical tag compares as different,
     * which makes {@code addAnnotation} stop rejecting duplicates and makes
     * {@code updateAnnotations} drop and re-add the entire set. Content equality makes the upgrade safe
     * in both directions and in either order.
     * Comparisons delegate to {@link #equals(String, String, String, String)} (case-insensitive,
     * URI-aware). Used by both {@code ExpressionExperimentService.updateAnnotations} and
     * {@code BioMaterialService.updateAnnotations} so the two diff implementations cannot drift.
     */
    public static boolean sameTag( Characteristic a, Characteristic b ) {
        if ( !equals( a.getCategory(), a.getCategoryUri(), b.getCategory(), b.getCategoryUri() )
                || !equals( a.getValue(), a.getValueUri(), b.getValue(), b.getValueUri() ) ) {
            return false;
        }
        // A non-Statement reads as all-null on the statement slots, so a plain Characteristic and a
        // Statement carrying no predicate/object compare equal, while either one differs from a
        // composed Statement.
        return equals( predicateOf( a ), predicateUriOf( a ), predicateOf( b ), predicateUriOf( b ) )
                && equals( objectOf( a ), objectUriOf( a ), objectOf( b ), objectUriOf( b ) )
                && equals( secondPredicateOf( a ), secondPredicateUriOf( a ), secondPredicateOf( b ), secondPredicateUriOf( b ) )
                && equals( secondObjectOf( a ), secondObjectUriOf( a ), secondObjectOf( b ), secondObjectUriOf( b ) );
    }

    @Nullable
    private static String predicateOf( Characteristic c ) {
        return c instanceof Statement ? ( ( Statement ) c ).getPredicate() : null;
    }

    @Nullable
    private static String predicateUriOf( Characteristic c ) {
        return c instanceof Statement ? ( ( Statement ) c ).getPredicateUri() : null;
    }

    @Nullable
    private static String objectOf( Characteristic c ) {
        return c instanceof Statement ? ( ( Statement ) c ).getObject() : null;
    }

    @Nullable
    private static String objectUriOf( Characteristic c ) {
        return c instanceof Statement ? ( ( Statement ) c ).getObjectUri() : null;
    }

    @Nullable
    private static String secondPredicateOf( Characteristic c ) {
        return c instanceof Statement ? ( ( Statement ) c ).getSecondPredicate() : null;
    }

    @Nullable
    private static String secondPredicateUriOf( Characteristic c ) {
        return c instanceof Statement ? ( ( Statement ) c ).getSecondPredicateUri() : null;
    }

    @Nullable
    private static String secondObjectOf( Characteristic c ) {
        return c instanceof Statement ? ( ( Statement ) c ).getSecondObject() : null;
    }

    @Nullable
    private static String secondObjectUriOf( Characteristic c ) {
        return c instanceof Statement ? ( ( Statement ) c ).getSecondObjectUri() : null;
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

    /**
     * Whether a supporting-evidence payload actually records something — i.e. whether it would survive
     * {@link #serializeSupportingEvidence}.
     * <p>
     * 🛑 The point is that {@code []} is NOT a record of anything, and must not be read as one. Every write
     * path on the curation route treats a null evidence field as "no change", so that a client which does not
     * carry provenance cannot wipe provenance somebody else recorded. An empty array is that same statement —
     * "I have none" — and a client building a payload from a reference file stamps it on every entity that has
     * no evidence, which is most of them. Testing {@code != null} lets that payload through the guard, and
     * because the serializer maps an empty tree to {@code null} the write then CLEARS the column: a wipe of
     * every stored block it touches, reported as an ordinary success.
     * <p>
     * So the guard asks this instead of asking for non-null. The consequence is that evidence cannot be
     * cleared through the commit route at all — which is the safe direction to be wrong in, and leaves an
     * explicit erase to be designed if one is ever wanted.
     */
    public static boolean hasRecordedEvidence( @Nullable JsonNode evidence ) {
        return serializeSupportingEvidence( evidence ) != null;
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
