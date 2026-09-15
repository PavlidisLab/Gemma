package ubic.gemma.model.expression.experiment;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.common.description.CharacteristicUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the design read path's provenance surface.
 * <p>
 * A {@link Statement} is a {@link ubic.gemma.model.common.description.Characteristic}, so its
 * {@code SUPPORTING_EVIDENCE} storage has always existed — but this value object did not expose it, which
 * meant the whole design section (factors → factor values → statements, the bulk of what curation produces)
 * could not answer "where did this term come from" even for rows that recorded an answer.
 */
public class StatementValueObjectTest {

    private static final String EVIDENCE = "[{\"quote\":\"organism part: stroma\",\"source\":\"characteristic\","
            + "\"location\":\"GSM1197956\"}]";

    @Test
    public void testSupportingEvidenceIsReadBackFromTheStatement() {
        Statement s = new Statement();
        s.setCategory( "organism part" );
        s.setSubject( "placental villous stroma" );
        s.setSubjectUri( "http://purl.obolibrary.org/obo/UBERON_8600023" );
        s.setSupportingEvidence( EVIDENCE );

        StatementValueObject vo = new StatementValueObject( s );

        assertThat( vo.getSupportingEvidence() ).isNotNull();
        assertThat( vo.getSupportingEvidence().isArray() ).isTrue();
        assertThat( vo.getSupportingEvidence().get( 0 ).get( "location" ).asText() ).isEqualTo( "GSM1197956" );
    }

    /** Nothing recorded is the expected reading, and must arrive as null rather than as an empty or an error. */
    @Test
    public void testAbsentSupportingEvidenceIsNull() {
        Statement s = new Statement();
        s.setCategory( "organism part" );
        s.setSubject( "chorionic villus" );

        assertThat( new StatementValueObject( s ).getSupportingEvidence() ).isNull();
    }

    /**
     * Evidence is provenance, not identity. The comparator is relied upon to assign annotation ids, and
     * equality feeds statement de-duplication, so the same statement with and without recorded evidence must
     * stay indistinguishable to both.
     */
    @Test
    public void testSupportingEvidenceAffectsNeitherEqualityNorOrdering() {
        Statement bare = new Statement();
        bare.setCategory( "organism part" );
        bare.setSubject( "placental villous stroma" );

        Statement evidenced = new Statement();
        evidenced.setCategory( "organism part" );
        evidenced.setSubject( "placental villous stroma" );
        evidenced.setSupportingEvidence( EVIDENCE );

        StatementValueObject a = new StatementValueObject( bare );
        StatementValueObject b = new StatementValueObject( evidenced );

        assertThat( b.getSupportingEvidence() ).isNotNull();
        assertThat( a ).isEqualTo( b ).hasSameHashCodeAs( b ).isEqualByComparingTo( b );
    }

    /** A malformed payload must not propagate a parse failure into a read response. */
    @Test
    public void testUnparseableSupportingEvidenceYieldsNullRatherThanThrowing() {
        Statement s = new Statement();
        s.setCategory( "organism part" );
        s.setSubject( "placental villous stroma" );
        s.setSupportingEvidence( "{not json" );

        assertThat( new StatementValueObject( s ).getSupportingEvidence() ).isNull();
        assertThat( CharacteristicUtils.parseSupportingEvidence( "{not json" ) ).isNull();
    }
}
