package ubic.gemma.model.expression.bioAssay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialValueObject;
import ubic.gemma.model.expression.experiment.Statement;


import static org.assertj.core.api.Assertions.assertThat;

/**
 * What {@code GET /datasets/{dataset}/samples} puts on the wire per assay.
 * <p>
 * That response is the experiment page's largest call — 5,381,688 bytes for the 278 assays of dataset
 * 3937 — and most of its bulk was structure rather than data: one platform serialized in full 278
 * times, and seven statement keys carrying null on all 5,291 statements. These pin the shape so a
 * field cannot drift back onto it unnoticed.
 *
 * @author gembro
 */
public class BioAssaySamplePayloadShapeTest {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * {@link ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject}'s constructor asks the
     * security context what to expose, so a bare unit test needs an authentication even though nothing
     * here is about authorization.
     */
    @BeforeEach
    public void authenticate() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken( "tester", "tester", "GROUP_USER" ) );
    }

    @AfterEach
    public void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    private static ArrayDesign platform() {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setId( 1L );
        ad.setShortName( "GPL96" );
        ad.setName( "Affymetrix GeneChip Human Genome U133 Array Set HG-U133A" );
        ad.setTechnologyType( TechnologyType.ONECOLOR );
        ad.setDescription( "a long platform description that has no business being repeated once per assay" );
        return ad;
    }

    private JsonNode serializeAssay() throws Exception {
        BioAssay ba = BioAssay.Factory.newInstance();
        ba.setId( 3000L );
        ba.setName( "GSM900001" );
        ba.setArrayDesignUsed( platform() );
        ba.setOriginalPlatform( platform() );
        return mapper.readTree( mapper.writeValueAsString( new BioAssayValueObject( ba ) ) );
    }

    /**
     * The platform is named, not reproduced. The full VO is byte-identical across every assay of a
     * dataset and was serialized once per assay: on dataset 3937 the two platform fields were 706,120
     * of 5,265,852 bytes, 13.4% of the response, most of it 278 copies of one 1.4 kB
     * {@code description}. {@code GET /datasets/{id}/platforms?original=true} serves the full object
     * once for a caller that wants the rest of it.
     */
    @Test
    public void anAssaysPlatformIsProjectedToTheReferenceShape() throws Exception {
        JsonNode ad = serializeAssay().get( "arrayDesign" );

        assertThat( ad ).isNotNull();
        assertThat( ad.fieldNames() ).toIterable()
                .containsExactlyInAnyOrder( "id", "shortName", "name", "technologyType" );
        assertThat( ad.get( "shortName" ).asText() ).isEqualTo( "GPL96" );
        assertThat( ad.get( "technologyType" ).asText() ).isEqualTo( "ONECOLOR" );
    }

    /** The same projection on the switched-from platform, which is the same object with the same bulk. */
    @Test
    public void anAssaysOriginalPlatformIsProjectedToo() throws Exception {
        JsonNode op = serializeAssay().get( "originalPlatform" );

        assertThat( op ).isNotNull();
        assertThat( op.fieldNames() ).toIterable()
                .containsExactlyInAnyOrder( "id", "shortName", "name", "technologyType" );
    }

    /**
     * A statement with nothing said about it says nothing, rather than saying null seven times.
     * <p>
     * This is the rule {@code AbstractFactorValueValueObjectSerializer#writeStatement} already applies
     * when it emits the same VO by hand on the factor-value path: null reads as "this was cleared",
     * and a subject-only statement has nothing to clear. The subject and category halves keep their
     * nulls on the same reasoning — they describe a term that IS there.
     */
    @Test
    public void aSubjectOnlyStatementOmitsThePredicateAndObjectHalves() throws Exception {
        Statement s = Statement.Factory.newInstance();
        s.setCategory( "organism part" );
        s.setSubject( "chorionic villus" );

        BioMaterial bm = BioMaterial.Factory.newInstance( "sample" );
        bm.setId( 1L );
        bm.getCharacteristics().add( s );

        JsonNode stmt = mapper.readTree( mapper.writeValueAsString( new BioMaterialValueObject( bm ) ) )
                .get( "statements" ).get( 0 );

        assertThat( stmt.has( "subject" ) ).isTrue();
        assertThat( stmt.has( "category" ) ).isTrue();
        assertThat( stmt.has( "subjectUri" ) )
                .withFailMessage( "an ungrounded subject is a term that IS there; its null says so" )
                .isTrue();
        for ( String said : new String[] { "predicate", "predicateUri", "object", "objectUri", "subjectId", "objectId" } ) {
            assertThat( stmt.has( said ) )
                    .withFailMessage( "nothing was said about this statement, so %s should be absent, not null", said )
                    .isFalse();
        }
    }

    /** And when something IS said, it is still said. */
    @Test
    public void aPredicatedStatementStillCarriesItsPredicateAndObject() throws Exception {
        Statement s = Statement.Factory.newInstance();
        s.setCategory( "genotype" );
        s.setSubject( "Trp53" );
        s.setPredicate( "has_genotype" );
        s.setObject( "Homozygous negative" );

        BioMaterial bm = BioMaterial.Factory.newInstance( "sample" );
        bm.setId( 1L );
        bm.getCharacteristics().add( s );

        JsonNode stmt = mapper.readTree( mapper.writeValueAsString( new BioMaterialValueObject( bm ) ) )
                .get( "statements" ).get( 0 );

        assertThat( stmt.get( "predicate" ).asText() ).isEqualTo( "has_genotype" );
        assertThat( stmt.get( "object" ).asText() ).isEqualTo( "Homozygous negative" );
    }

    /**
     * The deprecated {@code factors} map — factor id to factor name — is gone. It was superseded by the
     * {@code factorValues} collection, which carries the factor on each value, and had no reader in any
     * of the ten client repositories on this machine.
     */
    @Test
    public void aSampleNoLongerCarriesTheDeprecatedFactorsMap() throws Exception {
        BioMaterial bm = BioMaterial.Factory.newInstance( "sample" );
        bm.setId( 1L );

        JsonNode sample = mapper.readTree( mapper.writeValueAsString( new BioMaterialValueObject( bm ) ) );

        assertThat( sample.has( "factors" ) ).isFalse();
        assertThat( sample.has( "factorValues" ) )
                .withFailMessage( "the successor collection must survive the removal of its predecessor" )
                .isTrue();
    }

    /**
     * Statements are on by default. The opt-out drops them, and an opt-out that defaulted to off would
     * put predicated sample characteristics back out of sight — which is the thing the collection was
     * added to end.
     */
    @Test
    public void statementsAreAbsentRatherThanEmptyWhenExcluded() throws Exception {
        BioMaterial bm = BioMaterial.Factory.newInstance( "sample" );
        bm.setId( 1L );
        bm.getCharacteristics().add( Statement.Factory.newInstance() );

        BioMaterialValueObject vo = new BioMaterialValueObject( bm );
        assertThat( mapper.readTree( mapper.writeValueAsString( vo ) ).has( "statements" ) ).isTrue();

        vo.setStatements( null );
        JsonNode excluded = mapper.readTree( mapper.writeValueAsString( vo ) );
        assertThat( excluded.has( "statements" ) )
                .withFailMessage( "an empty array reads as \"this sample has no statements\", which is a "
                        + "different claim from \"you asked not to be sent them\"" )
                .isFalse();
    }
}
