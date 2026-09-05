package ubic.gemma.rest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.security.SecurityService;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.core.util.test.TestAuthenticationUtils;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;
import ubic.gemma.model.expression.experiment.StatementValueObject;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.AnnotationSetService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest5;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

/**
 * End-to-end regression harness for the composite curation commit
 * ({@code PUT /datasets/{id}/curation}) against a real (gemdtest) database — the persistence half the
 * mocked {@link DatasetsWebServiceTest} can't reach. Seeds one full experiment (design + factors +
 * factor-values + biomaterials + bioassays with GEO-style accessions), PUTs a real
 * {@code CurationDocument}, and asserts the persisted state on read-back.
 * <p>
 * Runs as admin (see {@link BaseJerseyIntegrationTest5}), so ACL edit + the admin-gated force path are
 * satisfied. Grows one method per section as the phases land (design → tags → sampleCharacteristics →
 * curationDetails).
 */
@Tag("integration")
@Tag("slow")
public class DatasetsCurationCommitRestTest extends BaseJerseyIntegrationTest5 {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private PersistentDummyObjectHelper testHelper;

    @Autowired
    private AnnotationSetService annotationSetService;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private ExpressionExperimentSubSetService expressionExperimentSubSetService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * The base class authenticates every test as admin in a {@code @BeforeEach}; switching identity inside a
     * test body is the only way to exercise a non-admin branch. Note {@code @WithMockUser} does NOT work here —
     * the base {@code @BeforeEach} would overwrite the context anyway.
     */
    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

    @Autowired
    private SecurityService securityService;

    private ExpressionExperiment ee;

    @BeforeEach
    public void seedExperiment() {
        testHelper.resetSeed();
        ee = testHelper.getTestExpressionExperimentWithAllDependencies( false );
    }

    @AfterEach
    public void removeExperiment() {
        if ( ee != null ) {
            expressionExperimentService.remove( ee );
        }
    }

    @Test
    public void testCommitDesignCreatesFactorAssignsSampleAndBaseline() {
        // A brand-new factor (no differential-expression analysis depends on it → no force needed).
        // Discover a real sample's GEO-style accession to target the assignment by short name.
        ExpressionExperiment thawed = expressionExperimentService.thawBioAssays( ee );
        BioAssay ba = thawed.getBioAssays().iterator().next();
        String gsm = ba.getAccession().getAccession();
        Long bmId = ba.getSampleUsed().getId();

        String body = "{"
                + "\"design\":{"
                + "\"factors\":{\"items\":[{"
                + "\"clientRef\":\"F1\",\"name\":\"treatment\",\"category\":{\"label\":\"treatment\"},"
                + "\"factorValues\":{\"items\":[{"
                + "\"clientRef\":\"FV1\",\"freeTextLabel\":\"drugX\",\"isBaseline\":true,"
                + "\"biomaterialShortNames\":[\"" + gsm + "\"]"
                + "}]}"
                + "}]},"
                + "\"shouldSplitOnFactorId\":-1,\"shouldSplitRationale\":\"single cohort\""
                + "}}";

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
            String json = r.readEntity( String.class );
            // clientRefs round-trip in the report idMap.
            assertThat( json ).contains( "idMap" ).contains( "F1" ).contains( "FV1" );
        }

        // Read-back: the new factor + factor-value are persisted, the FV is baseline and assigned to the sample.
        ExpressionExperiment reloaded = expressionExperimentService.load( ee.getId() );
        ExperimentalDesignValueObject design = expressionExperimentService.getExperimentalDesignValueObject( reloaded );

        ExperimentalDesignValueObject.ExperimentalFactorEntry treatment = design.getExperimentalFactors().stream()
                .filter( f -> "treatment".equals( f.getName() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "new 'treatment' factor was not persisted" ) );
        FactorValueBasicValueObject drugX = treatment.getValues().stream()
                .filter( v -> "drugX".equals( v.getValue() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "new 'drugX' factor value was not persisted" ) );
        assertThat( drugX.getBaseline() ).isTrue();

        // The sample is assigned to the new factor value.
        boolean assigned = design.getBioMaterialAssignments().stream()
                .filter( a -> bmId.equals( a.getBioMaterialId() ) )
                .anyMatch( a -> a.getFactorValueIds().contains( drugX.getId() ) );
        assertThat( assigned ).as( "sample %s assigned to new FV", gsm ).isTrue();

        // Split advice landed in the curation note (stopgap home).
        String note = reloaded.getCurationDetails() != null ? reloaded.getCurationDetails().getCurationNote() : null;
        assertThat( note ).contains( "[split-advice]" ).contains( "do not split" );
    }

    /**
     * Provenance at the FACTOR and FACTOR VALUE levels survives the commit.
     * <p>
     * Neither level had anywhere to land before: {@code supportingEvidence} existed on {@code TagCommit},
     * {@code StatementCommit} and {@code SampleCharacteristicCommit}, so a curator's justification for the
     * factor itself, or for a value's label / baseline / measurement, was accepted by Jackson and silently
     * dropped. Measured across the reference 500 that was 78 of 493 evidence blocks — 68 on factor values,
     * 10 on factors.
     * <p>
     * The statement evidence is asserted alongside on purpose: the three levels are separate slots, and the
     * failure this guards against is one of them being made to stand in for another.
     */
    @Test
    public void testCommitPersistsFactorAndFactorValueSupportingEvidence() {
        String body = "{"
                + "\"design\":{"
                + "\"factors\":{\"items\":[{"
                + "\"clientRef\":\"F1\",\"name\":\"tissue\",\"category\":{\"label\":\"organism part\"},"
                + "\"supportingEvidence\":[{\"quote\":\"villous stroma vs whole placenta\","
                + "\"source\":\"overall_design\",\"location\":\"GSE-design\"}],"
                + "\"factorValues\":{\"items\":[{"
                + "\"clientRef\":\"FV1\",\"freeTextLabel\":\"stroma\","
                + "\"supportingEvidence\":[{\"quote\":\"organism part: stroma\","
                + "\"source\":\"characteristic\",\"location\":\"GSM-fv\"}],"
                + "\"statements\":{\"items\":[{"
                + "\"clientRef\":\"S1\",\"category\":{\"label\":\"organism part\"},"
                + "\"subject\":{\"label\":\"placental villous stroma\","
                + "\"uri\":\"http://purl.obolibrary.org/obo/UBERON_8600023\"},"
                + "\"supportingEvidence\":[{\"quote\":\"stroma\",\"source\":\"characteristic\","
                + "\"location\":\"GSM-stmt\"}]}]}"
                + "}]}"
                + "}]}"
                + "}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertOk( r );
        }

        ExperimentalDesignValueObject design = expressionExperimentService
                .getExperimentalDesignValueObject( expressionExperimentService.load( ee.getId() ) );
        ExperimentalDesignValueObject.ExperimentalFactorEntry treatment = design.getExperimentalFactors().stream()
                .filter( f -> "tissue".equals( f.getName() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "new 'tissue' factor was not persisted" ) );

        assertThat( treatment.getSupportingEvidence() ).as( "factor-level evidence survived" ).isNotNull();
        assertThat( treatment.getSupportingEvidence().get( 0 ).get( "location" ).asText() )
                .isEqualTo( "GSE-design" );

        FactorValueBasicValueObject drugX = treatment.getValues().stream()
                .filter( v -> "stroma".equals( v.getValue() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "new 'stroma' factor value was not persisted" ) );

        assertThat( drugX.getSupportingEvidence() ).as( "factor-value-level evidence survived" ).isNotNull();
        assertThat( drugX.getSupportingEvidence().get( 0 ).get( "location" ).asText() ).isEqualTo( "GSM-fv" );

        // The three slots are distinct: the value did not inherit the statement's evidence, nor the reverse.
        assertThat( drugX.getStatements() ).hasSize( 1 );
        assertThat( drugX.getStatements().get( 0 ).getSupportingEvidence() ).isNotNull();
        assertThat( drugX.getStatements().get( 0 ).getSupportingEvidence().get( 0 ).get( "location" ).asText() )
                .as( "statement kept its own evidence, not the value's" )
                .isEqualTo( "GSM-stmt" );
    }

    /**
     * A second commit that omits {@code supportingEvidence} leaves the recorded evidence alone.
     * <p>
     * The design section is a carry-forward mapper: a client that mentions an existing factor value to change
     * one field re-sends the whole value, and the fields it does not carry arrive as null. Treating that null
     * as "clear it" would let any client with no provenance of its own erase provenance somebody else
     * recorded, just by editing a label. Same null = "no change" rule {@code value} and {@code isBaseline}
     * already follow.
     */
    @Test
    public void testOmittedEvidenceDoesNotWipeStoredFactorValueEvidence() {
        String seed = "{"
                + "\"design\":{\"factors\":{\"items\":[{"
                + "\"clientRef\":\"F1\",\"name\":\"treatment\",\"category\":{\"label\":\"treatment\"},"
                + "\"supportingEvidence\":[{\"quote\":\"drug X vs vehicle\",\"source\":\"overall_design\"}],"
                + "\"factorValues\":{\"items\":[{\"clientRef\":\"FV1\",\"freeTextLabel\":\"drugX\","
                + "\"supportingEvidence\":[{\"quote\":\"treatment: drug X\",\"source\":\"characteristic\"}]"
                + "}]}}]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( seed ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }

        ExperimentalDesignValueObject afterSeed = expressionExperimentService
                .getExperimentalDesignValueObject( expressionExperimentService.load( ee.getId() ) );
        ExperimentalDesignValueObject.ExperimentalFactorEntry seeded = afterSeed.getExperimentalFactors().stream()
                .filter( f -> "treatment".equals( f.getName() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seed factor was not persisted" ) );
        FactorValueBasicValueObject seededFv = seeded.getValues().stream()
                .filter( v -> "drugX".equals( v.getValue() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seed factor value was not persisted" ) );
        assertThat( seededFv.getSupportingEvidence() ).as( "precondition: evidence was stored" ).isNotNull();

        // Relabel the value, carrying no provenance at all.
        String relabel = "{"
                + "\"design\":{\"factors\":{\"items\":[{"
                + "\"gemmaId\":" + seeded.getId() + ","
                + "\"factorValues\":{\"items\":[{\"gemmaId\":" + seededFv.getId() + ","
                + "\"freeTextLabel\":\"drug X (10uM)\"}]}}]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( relabel ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }

        ExperimentalDesignValueObject after = expressionExperimentService
                .getExperimentalDesignValueObject( expressionExperimentService.load( ee.getId() ) );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = after.getExperimentalFactors().stream()
                .filter( f -> seeded.getId().equals( f.getId() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "factor disappeared" ) );
        FactorValueBasicValueObject fv = factor.getValues().stream()
                .filter( v -> seededFv.getId().equals( v.getId() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "factor value disappeared" ) );

        assertThat( fv.getValue() ).as( "the relabel did land" ).isEqualTo( "drug X (10uM)" );
        assertThat( fv.getSupportingEvidence() )
                .as( "omitting evidence did not erase the evidence already recorded" )
                .isNotNull();
        assertThat( factor.getSupportingEvidence() )
                .as( "the factor's evidence survived a commit that did not mention it" )
                .isNotNull();
    }

    /**
     * An EMPTY evidence array is "I have none", not "clear what is there".
     * <p>
     * The distinction is not academic: a payload built from a reference file stamps {@code []} on every entity
     * that has no evidence, which is most of them. Guarding on {@code != null} lets that through — an empty
     * array is not null — and the serializer maps an empty tree to {@code null}, so the write then clears the
     * column on every entity the payload touches and reports an ordinary success. CAB coerces {@code []} away
     * client-side for exactly this reason; the server must not depend on every client remembering to.
     * <p>
     * Asserted at all three levels because the guard is one predicate and a level left out of it is a level
     * where the wipe still happens.
     */
    @Test
    public void testAnEmptyEvidenceArrayDoesNotEraseStoredEvidence() {
        String seed = "{"
                + "\"design\":{\"factors\":{\"items\":[{"
                + "\"clientRef\":\"F1\",\"name\":\"tissue\",\"category\":{\"label\":\"organism part\"},"
                + "\"supportingEvidence\":[{\"quote\":\"villous stroma\",\"source\":\"overall_design\"}],"
                + "\"factorValues\":{\"items\":[{\"clientRef\":\"FV1\",\"freeTextLabel\":\"stroma\","
                + "\"supportingEvidence\":[{\"quote\":\"organism part: stroma\",\"source\":\"characteristic\"}],"
                + "\"statements\":{\"items\":[{\"clientRef\":\"S1\","
                + "\"category\":{\"label\":\"organism part\"},"
                + "\"subject\":{\"label\":\"placental villous stroma\","
                + "\"uri\":\"http://purl.obolibrary.org/obo/UBERON_8600023\"},"
                + "\"supportingEvidence\":[{\"quote\":\"stroma\",\"source\":\"characteristic\"}]}]}"
                + "}]}}]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( seed ) ) ) {
            assertOk( r );
        }

        ExperimentalDesignValueObject afterSeed = expressionExperimentService
                .getExperimentalDesignValueObject( expressionExperimentService.load( ee.getId() ) );
        ExperimentalDesignValueObject.ExperimentalFactorEntry seeded = afterSeed.getExperimentalFactors().stream()
                .filter( f -> "tissue".equals( f.getName() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seed factor was not persisted" ) );
        FactorValueBasicValueObject seededFv = seeded.getValues().stream()
                .filter( v -> "stroma".equals( v.getValue() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seed factor value was not persisted" ) );
        Long stmtId = seededFv.getStatements().get( 0 ).getId();
        assertThat( seeded.getSupportingEvidence() ).as( "precondition: factor evidence stored" ).isNotNull();
        assertThat( seededFv.getSupportingEvidence() ).as( "precondition: value evidence stored" ).isNotNull();
        assertThat( seededFv.getStatements().get( 0 ).getSupportingEvidence() )
                .as( "precondition: statement evidence stored" ).isNotNull();

        // Now re-send every level with an explicitly EMPTY evidence array.
        String wipe = "{"
                + "\"design\":{\"factors\":{\"items\":[{"
                + "\"gemmaId\":" + seeded.getId() + ",\"supportingEvidence\":[],"
                + "\"factorValues\":{\"items\":[{\"gemmaId\":" + seededFv.getId() + ","
                + "\"supportingEvidence\":[],"
                + "\"statements\":{\"items\":[{\"gemmaId\":" + stmtId + ",\"supportingEvidence\":[]}]}"
                + "}]}}]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( wipe ) ) ) {
            assertOk( r );
        }

        ExperimentalDesignValueObject after = expressionExperimentService
                .getExperimentalDesignValueObject( expressionExperimentService.load( ee.getId() ) );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = after.getExperimentalFactors().stream()
                .filter( f -> seeded.getId().equals( f.getId() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "factor disappeared" ) );
        FactorValueBasicValueObject fv = factor.getValues().stream()
                .filter( v -> seededFv.getId().equals( v.getId() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "factor value disappeared" ) );

        assertThat( factor.getSupportingEvidence() )
                .as( "[] did not erase the factor's evidence" ).isNotNull();
        assertThat( fv.getSupportingEvidence() )
                .as( "[] did not erase the factor value's evidence" ).isNotNull();
        assertThat( fv.getStatements() ).isNotEmpty();
        assertThat( fv.getStatements().get( 0 ).getSupportingEvidence() )
                .as( "[] did not erase the statement's evidence" ).isNotNull();
    }

    @Test
    public void testPreflightDesignWritesNothing() {
        long factorCountBefore = expressionExperimentService.getExperimentalDesignValueObject( ee )
                .getExperimentalFactors().size();

        String body = "{\"design\":{\"factors\":{\"items\":[{"
                + "\"clientRef\":\"F1\",\"name\":\"ghost\",\"category\":{\"label\":\"treatment\"},"
                + "\"factorValues\":{\"items\":[]}}]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/preflight" ).request().post( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
            assertThat( r.readEntity( String.class ) ).contains( "\"applied\":false" );
        }

        // Nothing persisted — the preview factor is absent.
        ExpressionExperiment reloaded = expressionExperimentService.load( ee.getId() );
        assertThat( expressionExperimentService.getExperimentalDesignValueObject( reloaded ).getExperimentalFactors() )
                .noneMatch( f -> "ghost".equals( f.getName() ) )
                .hasSize( (int) factorCountBefore );
    }

    @Test
    public void testCommitTagsAddThenDeleteById() {
        // Add an experiment-level tag by clientRef. URIs are required for it to survive the getAnnotations
        // read filter (which deliberately drops free-text tags), and the label must be the one the URI
        // actually resolves to — the commit gate rejects a label/URI mismatch (DOID_0060108 is "brain glioma",
        // not "glioma").
        String add = "{\"tags\":{\"items\":[{\"clientRef\":\"T1\","
                + "\"category\":{\"label\":\"disease\",\"uri\":\"http://purl.obolibrary.org/obo/DOID_4\"},"
                + "\"value\":{\"label\":\"brain glioma\",\"uri\":\"http://purl.obolibrary.org/obo/DOID_0060108\"}}]}}";
        String addJson;
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( add ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
            addJson = r.readEntity( String.class );
        }
        AnnotationValueObject glioma = findAnnotation( "brain glioma" );
        assertThat( glioma ).as( "tag was persisted" ).isNotNull();
        // idMap echoes the real new id, not null.
        assertThat( addJson ).contains( "\"T1\":" + glioma.getId() );

        // Delete it by its (now-known) id via deletedIds.
        String del = "{\"tags\":{\"items\":[],\"deletedIds\":[" + glioma.getId() + "]}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( del ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        assertThat( findAnnotation( "brain glioma" ) ).as( "tag was removed by deletedIds" ).isNull();
    }

    /**
     * The tags section had no transaction-boundary coverage for either provenance slot: the only guard was
     * {@link #testCommitPersistsStatementSupportingEvidence}, on design statements. A mapper that accepted
     * {@code supportingEvidence} and built a Characteristic without it, or a commit path that dropped it below
     * the REST layer, would have been invisible — the request returns 200 either way and the only way to learn
     * otherwise is to read the row back.
     * <p>
     * The evidence code is asserted in the same test because it shares that blind spot exactly, and because a
     * stated code is the whole point: without one the tag is recorded as {@code IC}, a curator's own inference,
     * whoever wrote it.
     */
    @Test
    public void testCommitPersistsTagSupportingEvidenceAndEvidenceCode() {
        String body = "{\"tags\":{\"items\":[{\"clientRef\":\"T1\","
                + "\"category\":{\"label\":\"disease\",\"uri\":\"http://purl.obolibrary.org/obo/DOID_4\"},"
                + "\"value\":{\"label\":\"brain glioma\",\"uri\":\"http://purl.obolibrary.org/obo/DOID_0060108\"},"
                + "\"evidenceCode\":\"IEA\","
                + "\"supportingEvidence\":[{\"quote\":\"disease: glioblastoma\",\"source\":\"characteristic\","
                + "\"location\":\"GSM1197956\"}]}]}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        AnnotationValueObject persisted = findAnnotation( "brain glioma" );
        assertThat( persisted ).as( "tag was persisted" ).isNotNull();
        assertThat( persisted.getSupportingEvidence() ).as( "evidence survived the round trip" ).isNotNull();
        assertThat( persisted.getSupportingEvidence().get( 0 ).get( "location" ).asText() ).isEqualTo( "GSM1197956" );
        assertThat( persisted.getEvidenceCode() ).isEqualTo( "IEA" );
    }

    /** Omitting the code leaves the add path's {@code IC} in place — the behaviour every tag on this route has had. */
    @Test
    public void testCommitWithoutAnEvidenceCodeStillRecordsIC() {
        String body = "{\"tags\":{\"items\":[{\"clientRef\":\"T1\","
                + "\"category\":{\"label\":\"disease\",\"uri\":\"http://purl.obolibrary.org/obo/DOID_4\"},"
                + "\"value\":{\"label\":\"brain glioma\",\"uri\":\"http://purl.obolibrary.org/obo/DOID_0060108\"}}]}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        AnnotationValueObject persisted = findAnnotation( "brain glioma" );
        assertThat( persisted ).as( "tag was persisted" ).isNotNull();
        assertThat( persisted.getEvidenceCode() ).isEqualTo( "IC" );
    }

    @Test
    public void testCommitSampleCharacteristic() {
        ExpressionExperiment thawed = expressionExperimentService.thawBioAssays( ee );
        BioAssay ba = thawed.getBioAssays().iterator().next();
        String gsm = ba.getAccession().getAccession();

        String body = "{\"sampleCharacteristics\":{\"items\":[{\"clientRef\":\"S1\",\"bioassayShortName\":\"" + gsm + "\","
                + "\"category\":{\"label\":\"genotype\"},\"value\":{\"label\":\"WT\"}}]}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
            // idMap echoes the real new id, not null.
            assertThat( r.readEntity( String.class ) ).contains( "S1" ).doesNotContain( "\"S1\":null" );
        }
        // Read the sample's characteristics back over HTTP.
        try ( Response r = target( "/datasets/" + ee.getId() + "/samples/" + ba.getId() + "/characteristics" ).request().get() ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
            assertThat( r.readEntity( String.class ) ).contains( "WT" );
        }
    }

    @Test
    public void testCommitCurationNote() {
        String body = "{\"curationDetails\":{\"curationNote\":\"integration note\"}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        ExpressionExperiment reloaded = expressionExperimentService.load( ee.getId() );
        assertThat( reloaded.getCurationDetails().getCurationNote() ).isEqualTo( "integration note" );
    }

    @Test
    public void testCurationDetailsTroubledIsRejected() {
        String body = "{\"curationDetails\":{\"troubled\":true}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.BAD_REQUEST.getStatusCode() );
        }
    }

    @Test
    public void testExistingFvWithNullSamplesKeepsAssignments() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        Long fvId = null, factorId = null;
        for ( ExperimentalDesignValueObject.ExperimentalFactorEntry f : before.getExperimentalFactors() ) {
            for ( FactorValueBasicValueObject v : f.getValues() ) {
                if ( countAssigned( before, v.getId() ) > 0 ) {
                    fvId = v.getId();
                    factorId = f.getId();
                    break;
                }
            }
            if ( fvId != null ) {
                break;
            }
        }
        assertThat( fvId ).as( "seeded design has an assigned factor value" ).isNotNull();
        long assignedBefore = countAssigned( before, fvId );

        // Re-send the factor + FV by id, change the FV label, OMIT biomaterialShortNames (null → leave samples).
        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":" + factorId + ","
                + "\"factorValues\":{\"items\":[{\"gemmaId\":" + fvId + ",\"freeTextLabel\":\"relabeled\"}]}}]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }

        ExperimentalDesignValueObject after = expressionExperimentService.getExperimentalDesignValueObject(
                expressionExperimentService.load( ee.getId() ) );
        assertThat( countAssigned( after, fvId ) ).as( "null samples left assignments untouched" ).isEqualTo( assignedBefore );
    }

    @Test
    public void testCommitAdvancesLastUpdated() {
        // Floor to the current second so the DB's timestamp precision can't cause a false failure.
        java.util.Date startOfSecond = new java.util.Date( ( System.currentTimeMillis() / 1000L ) * 1000L );
        // A basics-only change emits no section audit event — historically it left lastUpdated stuck at the seed time.
        String body = "{\"basics\":{\"description\":\"advance the concurrency token " + ee.getId() + "\"}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        java.util.Date lastUpdated = expressionExperimentService.load( ee.getId() ).getCurationDetails().getLastUpdated();
        assertThat( lastUpdated ).as( "commit advanced the lastUpdated token to ~now" ).isAfterOrEqualTo( startOfSecond );
    }

    // ============================================================================================
    // Gold write-back acceptance cases (handoffs/CAB_TO_GEMBRO_2026_08_16_GOLD_WRITE_BACK_CASES.md).
    //
    // W-numbers are that document's case labels. These are the ones that only mean something against a
    // real database: the foreign keys between BioMaterial, FactorValue and ExperimentalFactor are what
    // the "don't leave a mess" constraint is actually about, and a mock can't break them.
    // ============================================================================================

    /**
     * W1 — a factor is re-categorized by replacing it wholesale: the old factor goes out via {@code deletedIds}
     * and a new one arrives with {@code clientRef}, in ONE commit. The samples must end up on the new factor's
     * values with nothing left parented to the deleted factor.
     * <p>
     * This is the shape the curation side reaches for when every factor value is replaced anyway, so keeping the
     * parent id would preserve nothing real while making a changed factor look unchanged to everything holding a
     * reference.
     */
    @Test
    public void testCommitDeletesFactorAndRecreatesItInOneCommit() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry doomed = before.getExperimentalFactors().stream()
                .filter( f -> !f.getValues().isEmpty() )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seeded design has no factor with values" ) );
        Set<Long> doomedFvIds = doomed.getValues().stream().map( FactorValueBasicValueObject::getId )
                .collect( Collectors.toSet() );
        String gsm = anyGsm();

        String body = "{\"design\":{\"factors\":{"
                + "\"items\":[{\"clientRef\":\"F_NEW\",\"name\":\"individual\","
                + "\"category\":{\"label\":\"individual\",\"uri\":\"http://www.ebi.ac.uk/efo/EFO_0000542\"},"
                + "\"factorValues\":{\"items\":[{\"clientRef\":\"FV_NEW\",\"freeTextLabel\":\"H421\","
                + "\"biomaterialShortNames\":[\"" + gsm + "\"]}]}}],"
                + "\"deletedIds\":[" + doomed.getId() + "]"
                + "}}}";

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).queryParam( "force", true ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
            assertThat( r.readEntity( String.class ) ).contains( "F_NEW" ).contains( "FV_NEW" );
        }

        ExperimentalDesignValueObject after = reloadDesign();
        // the old factor is gone, and so is every factor value that hung off it
        assertThat( after.getExperimentalFactors() ).noneMatch( f -> doomed.getId().equals( f.getId() ) );
        assertThat( allFvIds( after ) ).doesNotContainAnyElementsOf( doomedFvIds );
        // no sample is still pointing at a deleted factor value
        assertThat( after.getBioMaterialAssignments() )
                .allSatisfy( a -> assertThat( a.getFactorValueIds() ).doesNotContainAnyElementsOf( doomedFvIds ) );
        // the replacement landed and carries the sample
        ExperimentalDesignValueObject.ExperimentalFactorEntry created = after.getExperimentalFactors().stream()
                .filter( f -> "individual".equals( f.getName() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "replacement factor was not created" ) );
        assertThat( created.getValues() ).hasSize( 1 );
        assertThat( countAssigned( after, created.getValues().get( 0 ).getId() ) ).isEqualTo( 1 );
    }

    /**
     * W6 — the most foreign-key-sensitive case on the list: within one factor, some samples move OUT of an
     * existing factor value INTO a newly created one, while the donor factor value survives (it is neither
     * deleted nor recreated) and a sibling is relabelled in place.
     */
    @Test
    public void testCommitMovesSamplesFromAnExistingFactorValueToANewOne() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        Map<Long, String> gsmByBmId = gsmByBioMaterialId();

        // a factor value carrying at least two samples, so one can leave and one can stay
        ExperimentalDesignValueObject.ExperimentalFactorEntry found = null;
        FactorValueBasicValueObject foundDonor = null;
        for ( ExperimentalDesignValueObject.ExperimentalFactorEntry f : before.getExperimentalFactors() ) {
            for ( FactorValueBasicValueObject v : f.getValues() ) {
                if ( countAssigned( before, v.getId() ) >= 2 ) {
                    found = f;
                    foundDonor = v;
                    break;
                }
            }
            if ( foundDonor != null ) break;
        }
        assumeThat( foundDonor ).as( "seeded design has a factor value with >= 2 samples" ).isNotNull();
        final ExperimentalDesignValueObject.ExperimentalFactorEntry factor = found;
        final FactorValueBasicValueObject donor = foundDonor;

        List<Long> donorBmIds = before.getBioMaterialAssignments().stream()
                .filter( a -> a.getFactorValueIds().contains( donor.getId() ) )
                .map( ExperimentalDesignValueObject.BioMaterialFactorValueAssignment::getBioMaterialId )
                .collect( Collectors.toList() );
        Long movingBmId = donorBmIds.get( 0 );
        List<Long> stayingBmIds = donorBmIds.subList( 1, donorBmIds.size() );
        long donorCountBefore = donorBmIds.size();

        String stayingNames = stayingBmIds.stream()
                .map( id -> "\"" + gsmByBmId.get( id ) + "\"" )
                .collect( Collectors.joining( "," ) );

        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":" + factor.getId() + ","
                + "\"factorValues\":{\"items\":["
                // the donor survives by id, shrunk to the samples that stay
                + "{\"gemmaId\":" + donor.getId() + ",\"biomaterialShortNames\":[" + stayingNames + "]},"
                // and a brand-new factor value takes the one that moved
                + "{\"clientRef\":\"FV_SPLIT\",\"freeTextLabel\":\"S1pr1 sphingosine-1-phosphate receptor\","
                + "\"biomaterialShortNames\":[\"" + gsmByBmId.get( movingBmId ) + "\"]}"
                + "]}}]}}}";

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).queryParam( "force", true ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }

        ExperimentalDesignValueObject after = reloadDesign();
        // the donor kept its id — it was shrunk, not deleted and recreated
        assertThat( allFvIds( after ) ).contains( donor.getId() );
        assertThat( countAssigned( after, donor.getId() ) ).isEqualTo( donorCountBefore - 1 );
        // the moved sample sits on the new factor value, under the same factor
        ExperimentalDesignValueObject.ExperimentalFactorEntry sameFactor = factorById( after, factor.getId() );
        FactorValueBasicValueObject split = sameFactor.getValues().stream()
                .filter( v -> !donor.getId().equals( v.getId() ) )
                .filter( v -> countAssigned( after, v.getId() ) == 1 )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "the new factor value was not created under the same factor" ) );
        assertThat( after.getBioMaterialAssignments() ).anySatisfy( a -> {
            assertThat( a.getBioMaterialId() ).isEqualTo( movingBmId );
            assertThat( a.getFactorValueIds() ).contains( split.getId() ).doesNotContain( donor.getId() );
        } );
    }

    /**
     * W4 — one factor value is re-termed in place and every sibling is left alone. This is the case where
     * preserving identity is required: an API that could only delete and recreate at factor granularity would
     * make a one-term correction destructive.
     */
    @Test
    public void testCommitRetermsOneFactorValueLeavingSiblingsUntouched() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = before.getExperimentalFactors().stream()
                .filter( f -> f.getValues().size() >= 2 )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seeded design has no factor with >= 2 values" ) );
        FactorValueBasicValueObject target = factor.getValues().get( 0 );
        FactorValueBasicValueObject sibling = factor.getValues().get( 1 );
        String siblingSummaryBefore = sibling.getSummary();
        long siblingSamplesBefore = countAssigned( before, sibling.getId() );
        assumeThat( target.getStatements() ).as( "seeded factor value carries a statement to re-term" ).isNotEmpty();
        Long oldStatementId = target.getStatements().get( 0 ).getId();

        // A re-term under declared-delete semantics: the replacement arrives in items, the approximation it
        // supersedes leaves via deletedIds. Absence alone never deletes.
        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":" + factor.getId() + ","
                + "\"factorValues\":{\"items\":[{\"gemmaId\":" + target.getId() + ",\"statements\":{"
                + "\"items\":[{\"clientRef\":\"S1\",\"category\":{\"label\":\"organism part\"},"
                + "\"subject\":{\"label\":\"placental villous stroma\",\"uri\":\"http://purl.obolibrary.org/obo/UBERON_8600023\"}}],"
                + "\"deletedIds\":[" + oldStatementId + "]"
                + "}}]}}]}}}";

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }

        ExperimentalDesignValueObject after = reloadDesign();
        ExperimentalDesignValueObject.ExperimentalFactorEntry sameFactor = factorById( after, factor.getId() );
        // the factor kept its id and its full complement of values
        assertThat( sameFactor.getValues() ).hasSize( factor.getValues().size() );
        FactorValueBasicValueObject reTermed = fvById( sameFactor, target.getId() );
        assertThat( reTermed.getStatements() ).singleElement()
                .satisfies( s -> assertThat( s.getSubject() ).isEqualTo( "placental villous stroma" ) );
        // and the sibling is byte-identical, samples included
        FactorValueBasicValueObject siblingAfter = fvById( sameFactor, sibling.getId() );
        assertThat( siblingAfter.getSummary() ).isEqualTo( siblingSummaryBefore );
        assertThat( countAssigned( after, sibling.getId() ) ).isEqualTo( siblingSamplesBefore );
    }

    /**
     * A statement {@code deletedIds} entry naming a row that is not on that factor value is refused rather
     * than absorbed. It used to answer 200 with {@code deleted: 0}: the delete is a suppression of the
     * carry-forward, so an id that was never carried forward suppresses nothing and reads exactly like a
     * delete that worked. A caller recorded eight such deletions against eid 6146 on 2026-09-01.
     */
    @Test
    public void testCommitRefusesStatementDeletedIdThatIsNotOnThatFactorValue() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = before.getExperimentalFactors().stream()
                .filter( f -> !f.getValues().isEmpty() )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seeded design has no factor with values" ) );
        FactorValueBasicValueObject target = factor.getValues().get( 0 );

        // 987654321 is a well-formed id that is simply not one of this factor value's statements.
        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":" + factor.getId() + ","
                + "\"factorValues\":{\"items\":[{\"gemmaId\":" + target.getId() + ",\"statements\":{"
                + "\"items\":[],\"deletedIds\":[987654321]"
                + "}}]}}]}}}";

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.BAD_REQUEST.getStatusCode() );
            assertThat( r.readEntity( String.class ) ).contains( "987654321" );
        }
    }

    /**
     * The dry run refuses it too, so a client can find out its document is stale without writing anything.
     */
    @Test
    public void testDryRunRefusesTheSameUnmatchedDeletedId() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = before.getExperimentalFactors().stream()
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seeded design has no factors" ) );

        String body = "{\"design\":{\"factors\":{\"items\":[],\"deletedIds\":[987654321]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).queryParam( "dryRun", true )
                .request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.BAD_REQUEST.getStatusCode() );
        }
    }

    /**
     * W11 — a factor description-only edit reaches the database. Nothing structural moves, so this used to be
     * short-circuited as a no-op and dropped without a word; the client saw 200 and no change.
     */
    @Test
    public void testCommitFactorDescriptionOnlyEditPersists() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = before.getExperimentalFactors().stream()
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seeded design has no factors" ) );

        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":" + factor.getId() + ","
                + "\"description\":\"Mycn oe, Myc oe\"}]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }

        ExperimentalDesignValueObject after = reloadDesign();
        assertThat( factorById( after, factor.getId() ).getDescription() ).isEqualTo( "Mycn oe, Myc oe" );
        // a metadata-only edit leaves the values alone
        assertThat( factorById( after, factor.getId() ).getValues() ).hasSize( factor.getValues().size() );
    }

    /**
     * W14 — a continuous factor value keeps its measurement AND its unit. {@code Measurement.unit} does not
     * cascade on persist, so a unit that isn't resolved to a persistent row lands as a bare number.
     */
    @Test
    public void testCommitContinuousFactorValueCarriesMeasurementAndUnit() {
        String gsm = anyGsm();
        // continuous factor values carry a measurement instead of a label, and cannot be a baseline
        String body = "{\"design\":{\"factors\":{\"items\":[{\"clientRef\":\"F_T\",\"name\":\"timepoint\","
                + "\"type\":\"continuous\",\"category\":{\"label\":\"timepoint\"},"
                + "\"factorValues\":{\"items\":[{\"clientRef\":\"FV_T\","
                + "\"measurement\":{\"value\":\"37\",\"unit\":\"day\",\"type\":\"ABSOLUTE\",\"representation\":\"DOUBLE\"},"
                + "\"biomaterialShortNames\":[\"" + gsm + "\"]}]}}]}}}";

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }

        ExperimentalDesignValueObject after = reloadDesign();
        FactorValueBasicValueObject fv = after.getExperimentalFactors().stream()
                .filter( f -> "timepoint".equals( f.getName() ) )
                .flatMap( f -> f.getValues().stream() )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "continuous factor value was not persisted" ) );
        assertThat( fv.getMeasurementObject() ).isNotNull();
        assertThat( fv.getMeasurementObject().getValue() ).isEqualTo( "37" );
        assertThat( fv.getMeasurementObject().getUnit() ).as( "the unit survived the persist" ).isEqualTo( "day" );
    }

    /**
     * W3 — deleting a continuous factor takes its measurement-bearing factor values with it. Worth its own case
     * because continuous factor values carry {@code Measurement} rows that categorical ones don't.
     */
    @Test
    public void testCommitDeletesContinuousFactorWithItsMeasurementValues() {
        String gsm = anyGsm();
        String create = "{\"design\":{\"factors\":{\"items\":[{\"clientRef\":\"F_RIN\",\"name\":\"RIN\","
                + "\"type\":\"continuous\",\"category\":{\"label\":\"collection of material\"},"
                + "\"factorValues\":{\"items\":[{\"clientRef\":\"FV_RIN\","
                + "\"measurement\":{\"value\":\"9.7\",\"unit\":\"RIN\",\"type\":\"ABSOLUTE\",\"representation\":\"DOUBLE\"},"
                + "\"biomaterialShortNames\":[\"" + gsm + "\"]}]}}]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( create ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }

        ExperimentalDesignValueObject seeded = reloadDesign();
        ExperimentalDesignValueObject.ExperimentalFactorEntry rin = seeded.getExperimentalFactors().stream()
                .filter( f -> "RIN".equals( f.getName() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "continuous factor was not created" ) );
        Set<Long> rinFvIds = rin.getValues().stream().map( FactorValueBasicValueObject::getId )
                .collect( Collectors.toSet() );
        assertThat( rinFvIds ).isNotEmpty();

        String delete = "{\"design\":{\"factors\":{\"items\":[],\"deletedIds\":[" + rin.getId() + "]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).queryParam( "force", true ).request().put( Entity.json( delete ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }

        ExperimentalDesignValueObject after = reloadDesign();
        assertThat( after.getExperimentalFactors() ).noneMatch( f -> rin.getId().equals( f.getId() ) );
        assertThat( allFvIds( after ) ).doesNotContainAnyElementsOf( rinFvIds );
        assertThat( after.getBioMaterialAssignments() )
                .allSatisfy( a -> assertThat( a.getFactorValueIds() ).doesNotContainAnyElementsOf( rinFvIds ) );
    }

    /**
     * W8 / W16 — deliberately ungrounded free text (JAX strain nomenclature has no ontology term, and the
     * curation side files these as free text on purpose) survives with its characters intact. The non-ASCII half
     * matters because our GEO import mangled unicode in 68 experiments and the curated text is the repair.
     */
    @Test
    public void testCommitPreservesUngroundedAndNonAsciiFreeText() {
        String gsm = anyGsm();
        String curated = "129SvEv-Tac/C57BL/6 · 10 µM β-estradiol · 37 °C";

        String body = "{\"design\":{\"factors\":{\"items\":[{\"clientRef\":\"F_S\",\"name\":\"strain\","
                + "\"category\":{\"label\":\"strain\"},"
                + "\"factorValues\":{\"items\":[{\"clientRef\":\"FV_S\",\"freeTextLabel\":\"" + curated + "\","
                + "\"biomaterialShortNames\":[\"" + gsm + "\"]}]}}]}}}";

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).as( "ungrounded free text is not a grounding failure" )
                    .isEqualTo( Response.Status.OK.getStatusCode() );
        }

        ExperimentalDesignValueObject after = reloadDesign();
        FactorValueBasicValueObject fv = after.getExperimentalFactors().stream()
                .filter( f -> "strain".equals( f.getName() ) )
                .flatMap( f -> f.getValues().stream() )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "ungrounded factor value was not persisted" ) );
        assertThat( fv.getValue() ).as( "free text round-tripped byte-exact, not coerced or re-mangled" )
                .isEqualTo( curated );
    }

    /**
     * W13 — a curated statement's justification survives the real persist. Until now the composite commit had
     * nowhere to put one on any section, so curation written through this path arrived stripped of the reason
     * it was made and acquired only "modified by the API user".
     */
    @Test
    public void testCommitPersistsStatementSupportingEvidence() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = before.getExperimentalFactors().stream()
                .filter( f -> !f.getValues().isEmpty() )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seeded design has no factor with values" ) );
        FactorValueBasicValueObject target = factor.getValues().get( 0 );

        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":" + factor.getId() + ","
                + "\"factorValues\":{\"items\":[{\"gemmaId\":" + target.getId() + ",\"statements\":{\"items\":[{"
                + "\"clientRef\":\"S1\",\"category\":{\"label\":\"organism part\"},"
                + "\"subject\":{\"label\":\"placental villous stroma\",\"uri\":\"http://purl.obolibrary.org/obo/UBERON_8600023\"},"
                + "\"supportingEvidence\":[{\"quote\":\"organism part: stroma\",\"source\":\"characteristic\","
                + "\"location\":\"GSM1197956\"}]"
                + "}]}}]}}]}}}";

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }

        StatementValueObject persisted = fvById( factorById( reloadDesign(), factor.getId() ), target.getId() )
                .getStatements().stream()
                .filter( s -> "placental villous stroma".equals( s.getSubject() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "the curated statement was not persisted" ) );
        assertThat( persisted.getSupportingEvidence() ).as( "evidence survived the round trip" ).isNotNull();
        assertThat( persisted.getSupportingEvidence().get( 0 ).get( "location" ).asText() ).isEqualTo( "GSM1197956" );
    }

    /**
     * A change that would strand a subset on deleted factor values needs the same explicit consent as the
     * analysis cascade: 409 without {@code force}, applied with it. The stranded subset is the more dangerous
     * of the two because it survives the change still looking valid.
     */
    @Test
    public void testCommitStrandingASubsetRequiresForce() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = factorWithAnAssignedValue( before );
        FactorValueBasicValueObject anchor = firstAssignedValue( before, factor );
        ExpressionExperimentSubSet subset = anchorSubsetOn( before, anchor );
        assertThat( subset ).isNotNull();

        String body = deleteFactorValue( factor.getId(), anchor.getId() );

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).as( "stranding a subset is a consent question, not a silent success" )
                    .isEqualTo( Response.Status.CONFLICT.getStatusCode() );
            String json = r.readEntity( String.class );
            assertThat( json ).contains( "subset" );
            assertThat( json ).as( "and the client is told which 409 this is, not left to read the sentence" )
                    .contains( "\"reason\":\"REQUIRES_FORCE\"" );
        }
        // nothing was written
        assertThat( allFvIds( reloadDesign() ) ).contains( anchor.getId() );

        // and the same request with consent goes through
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).queryParam( "force", true )
                .request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        assertThat( allFvIds( reloadDesign() ) ).doesNotContain( anchor.getId() );

        expressionExperimentSubSetService.remove( subset );
    }

    // ============================================================================================
    // Snapshot → change → compare → restore round trips.
    //
    // The backup exists for one workflow: capture the old Gemma annotations, let an agent apply
    // everything it finds, then compare or put it back. These tests walk that whole loop, because the
    // interesting failures are all in the seam between capture and replay, not in either half alone.
    // ============================================================================================

    /**
     * The basic loop: snapshot, relabel a factor value, then restore and find the original label back. Nothing
     * structural moves, so every id survives and the restore is a true revert.
     */
    @Test
    public void testSnapshotThenRelabelThenRestoreReturnsTheOriginalLabel() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = before.getExperimentalFactors().stream()
                .filter( f -> !f.getValues().isEmpty() )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seeded design has no factor with values" ) );
        FactorValueBasicValueObject target = factor.getValues().get( 0 );
        String originalSummary = target.getSummary();

        long setId = takeSnapshot();

        // the agent relabels it
        String edit = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":" + factor.getId() + ","
                + "\"factorValues\":{\"items\":[{\"gemmaId\":" + target.getId() + ",\"freeTextLabel\":\"agent relabelled\"}]}}]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( edit ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        assertThat( fvById( factorById( reloadDesign(), factor.getId() ), target.getId() ).getValue() )
                .isEqualTo( "agent relabelled" );

        // compare: the dry run reports the difference without writing
        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/" + setId + "/restore" )
                .queryParam( "dryRun", true ).queryParam( "force", true ).request().post( Entity.json( "" ) ) ) {
            assertOk( r );
        }
        assertThat( fvById( factorById( reloadDesign(), factor.getId() ), target.getId() ).getValue() )
                .as( "a dry-run compare writes nothing" ).isEqualTo( "agent relabelled" );

        // restore
        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/" + setId + "/restore" )
                .queryParam( "force", true ).request().post( Entity.json( "" ) ) ) {
            assertOk( r );
        }

        ExperimentalDesignValueObject after = reloadDesign();
        FactorValueBasicValueObject restored = fvById( factorById( after, factor.getId() ), target.getId() );
        assertThat( restored.getValue() ).isNotEqualTo( "agent relabelled" );
        assertThat( restored.getSummary() ).as( "the original label is back, on the same id" ).isEqualTo( originalSummary );
    }

    /**
     * A restore has to undo additions too, not just edits. The commit is declared-delete, so anything the agent
     * added since the snapshot has to be named in {@code deletedIds} by the reconciliation — otherwise it
     * silently survives a "restore" and the dataset is left in a state that matches no snapshot at all.
     */
    @Test
    public void testRestoreRemovesAFactorTheAgentAddedAfterTheSnapshot() {
        int factorsBefore = expressionExperimentService.getExperimentalDesignValueObject( ee )
                .getExperimentalFactors().size();
        long setId = takeSnapshot();

        String gsm = anyGsm();
        String add = "{\"design\":{\"factors\":{\"items\":[{\"clientRef\":\"F_AGENT\",\"name\":\"agent added\","
                + "\"category\":{\"label\":\"treatment\"},"
                + "\"factorValues\":{\"items\":[{\"clientRef\":\"FV_AGENT\",\"freeTextLabel\":\"drugZ\","
                + "\"biomaterialShortNames\":[\"" + gsm + "\"]}]}}]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( add ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        assertThat( reloadDesign().getExperimentalFactors() ).hasSize( factorsBefore + 1 );

        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/" + setId + "/restore" )
                .queryParam( "force", true ).request().post( Entity.json( "" ) ) ) {
            assertOk( r );
        }

        ExperimentalDesignValueObject after = reloadDesign();
        assertThat( after.getExperimentalFactors() ).hasSize( factorsBefore );
        assertThat( after.getExperimentalFactors() ).noneMatch( f -> "agent added".equals( f.getName() ) );
    }

    /**
     * The identity caveat, pinned as behaviour rather than left in a doc comment. When the agent deletes a
     * factor and the snapshot is replayed, the factor comes back by content under a NEW id — the row the
     * snapshot named is gone and no amount of replay resurrects it. A caller that assumes restore is an
     * id-for-id revert would be wrong, and this is where they find out.
     */
    @Test
    public void testRestoreAfterADeleteReturnsContentUnderANewId() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry doomed = before.getExperimentalFactors().stream()
                .filter( f -> !f.getValues().isEmpty() && f.getName() != null )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seeded design has no named factor with values" ) );
        String doomedName = doomed.getName();
        int valueCount = doomed.getValues().size();

        long setId = takeSnapshot();

        String delete = "{\"design\":{\"factors\":{\"items\":[],\"deletedIds\":[" + doomed.getId() + "]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).queryParam( "force", true )
                .request().put( Entity.json( delete ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        assertThat( reloadDesign().getExperimentalFactors() ).noneMatch( f -> doomed.getId().equals( f.getId() ) );

        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/" + setId + "/restore" )
                .queryParam( "force", true ).request().post( Entity.json( "" ) ) ) {
            assertOk( r );
        }

        ExperimentalDesignValueObject after = reloadDesign();
        ExperimentalDesignValueObject.ExperimentalFactorEntry rebuilt = after.getExperimentalFactors().stream()
                .filter( f -> doomedName.equals( f.getName() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "the deleted factor did not come back" ) );
        assertThat( rebuilt.getValues() ).as( "content restored" ).hasSize( valueCount );
        assertThat( rebuilt.getId() ).as( "but under a new id — a restore is not an id-for-id revert" )
                .isNotEqualTo( doomed.getId() );
    }

    /** Restoring an unchanged dataset changes nothing: the snapshot round-trips as a no-op. */
    @Test
    public void testRestoringAnUnchangedDatasetIsANoOp() {
        long setId = takeSnapshot();
        ExperimentalDesignValueObject before = reloadDesign();

        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/" + setId + "/restore" )
                .queryParam( "force", true ).request().post( Entity.json( "" ) ) ) {
            assertOk( r );
        }

        ExperimentalDesignValueObject after = reloadDesign();
        assertThat( allFvIds( after ) ).as( "every factor value kept its id" ).isEqualTo( allFvIds( before ) );
        assertThat( after.getExperimentalFactors() ).hasSize( before.getExperimentalFactors().size() );
    }

    /** Only a SNAPSHOT can be restored — replaying some other tool's payload shape as a commit is a 400. */
    @Test
    public void testRestoringANonSnapshotIsRejected() {
        String draft = "{\"payloadJson\":\"{}\"}";
        long draftId;
        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/draft" ).request()
                .put( Entity.json( draft ) ) ) {
            assertThat( r.getStatus() ).isIn( Response.Status.OK.getStatusCode(), Response.Status.CREATED.getStatusCode() );
            draftId = Long.parseLong( r.readEntity( String.class ).replaceAll( "(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1" ) );
        }
        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/" + draftId + "/restore" )
                .request().post( Entity.json( "" ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.BAD_REQUEST.getStatusCode() );
        }
    }

    /**
     * {@code force=true} is not a bypass. A non-admin cannot push a destructive design change through, and
     * nothing is written when they try.
     * <p>
     * Scope, so nobody inherits a hunt with no prize at the end of it: <b>in production every curator is an
     * admin</b> (Paul, 2026-08-16), and only admins delete factors. The non-admin this test constructs is
     * therefore not a user class that exists, and the branch it exercises is not one real users reach. That is
     * also why the other tests here pass: an admin gets no ACL restriction clause at all, so the voter never
     * consults an ACE.
     * <p>
     * Still worth keeping as defence in depth — it pins that {@code force=true} cannot be used to push a
     * destructive change through as a non-admin. It asserts the outcome (not 200, nothing written) rather than
     * a status code, because the layer that refuses is not the contract: today it is a <b>403</b> from the ACL,
     * so the request never reaches the admin-only force gate ({@code force && isUserAdmin()}) at all.
     * <p>
     * Findings left here because each killed a plausible wrong answer, and one of them was mine. The ACL grant
     * <b>works</b> — {@code makeOwnedByUser} writes explicit WRITE/READ ACEs and the assertion below confirms
     * it — so this is not "ownership doesn't imply edit". Sid mismatch, ACL caching, transaction visibility and
     * context propagation are all eliminated. What remains: the voter is asked about the <b>factor</b>, not the
     * experiment, because {@code ObjectIdentityRetrievalStrategyImpl} builds the identity from the domain
     * object and does not resolve a {@code SecuredChild} to its parent — leaving inheritance to the factor's
     * own ACL row ({@code parent_object} + {@code entries_inheriting}). Unverified, and closed as
     * not-worth-chasing: it has no operational consequence while all curators are admins.
     * <p>
     * Worth having because every other test in this class runs as admin — {@code BaseJerseyIntegrationTest5}
     * authenticates in a {@code @BeforeEach} — so without switching identity inside the test body the admin
     * branch is the only one ever exercised and {@code force=true} would look unconditional. ({@code
     * @WithMockUser} does not work here: the base {@code @BeforeEach} overwrites the context.)
     */
    @Test
    public void testForceIsNotABypassForANonAdmin() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry doomed = before.getExperimentalFactors().stream()
                .filter( f -> !f.getValues().isEmpty() )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seeded design has no factor with values" ) );
        Set<Long> doomedFvIds = doomed.getValues().stream().map( FactorValueBasicValueObject::getId )
                .collect( Collectors.toSet() );

        String curator = "curationforcetest";
        testAuthenticationUtils.runAsUser( curator, true );
        testAuthenticationUtils.runAsAdmin();
        securityService.makeOwnedByUser( ee, curator );
        // Record what the ACL actually thinks, rather than inferring it from the status code below. This is the
        // difference between "the curator was denied" and "the curator was never granted", and the two want
        // different fixes if this test ever starts failing.
        assertThat( securityService.isEditableByUser( ee, curator ) )
                .as( "the ACL grant took effect — so a refusal below is not a missing grant" )
                .isTrue();

        String delete = "{\"design\":{\"factors\":{\"items\":[],\"deletedIds\":[" + doomed.getId() + "]}}}";
        try {
            testAuthenticationUtils.runAsUser( curator, false );
            try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).queryParam( "force", true )
                    .request().put( Entity.json( delete ) ) ) {
                assertThat( r.getStatus() ).as( "force is not a bypass for a non-admin" )
                        .isNotEqualTo( Response.Status.OK.getStatusCode() );
            }
        } finally {
            // the teardown deletes the experiment, which needs admin back
            testAuthenticationUtils.runAsAdmin();
        }
        assertThat( allFvIds( reloadDesign() ) ).as( "and nothing was written" ).containsAll( doomedFvIds );

        // the same request as admin goes through, so the refusal above was about the caller and not the payload
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).queryParam( "force", true )
                .request().put( Entity.json( delete ) ) ) {
            assertOk( r );
        }
        assertThat( reloadDesign().getExperimentalFactors() ).noneMatch( f -> doomed.getId().equals( f.getId() ) );
    }

    /** The first factor carrying a value that samples are actually assigned to. */
    private static ExperimentalDesignValueObject.ExperimentalFactorEntry factorWithAnAssignedValue(
            ExperimentalDesignValueObject design ) {
        return design.getExperimentalFactors().stream()
                .filter( f -> f.getValues().stream().anyMatch( v -> countAssigned( design, v.getId() ) > 0 ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seeded design has no assigned factor value" ) );
    }

    private static FactorValueBasicValueObject firstAssignedValue( ExperimentalDesignValueObject design,
            ExperimentalDesignValueObject.ExperimentalFactorEntry factor ) {
        return factor.getValues().stream()
                .filter( v -> countAssigned( design, v.getId() ) > 0 )
                .findFirst()
                .orElseThrow();
    }

    /**
     * A subset defined by exactly the samples carrying one factor value, so that deleting that value strands it.
     * This is how these tests make {@code requiresForce()} true: the seeded experiment carries no
     * differential-expression analyses, and stranding a subset is the other half of the predicate.
     * <p>
     * Built inside a transaction because the create cascades over each BioAssay's sample and its lazy
     * factor-value collections, which are detached once {@code thawBioAssays}' session closes.
     */
    private ExpressionExperimentSubSet anchorSubsetOn( ExperimentalDesignValueObject design,
            FactorValueBasicValueObject anchor ) {
        Set<Long> anchorBmIds = design.getBioMaterialAssignments().stream()
                .filter( a -> a.getFactorValueIds().contains( anchor.getId() ) )
                .map( ExperimentalDesignValueObject.BioMaterialFactorValueAssignment::getBioMaterialId )
                .collect( Collectors.toSet() );
        return new TransactionTemplate( transactionManager ).execute( status -> {
            ExpressionExperiment attached = expressionExperimentService.thawBioAssays(
                    expressionExperimentService.load( ee.getId() ) );
            ExpressionExperimentSubSet ss = ExpressionExperimentSubSet.Factory
                    .newInstance( "anchored on " + anchor.getId(), attached );
            for ( BioAssay ba : attached.getBioAssays() ) {
                if ( ba.getSampleUsed() != null && anchorBmIds.contains( ba.getSampleUsed().getId() ) ) {
                    ss.getBioAssays().add( ba );
                }
            }
            return expressionExperimentSubSetService.create( ss );
        } );
    }

    /**
     * Assert a 200, and put the response body in the failure message when it isn't. A commit rejection carries
     * its reason in the body — a bare "expected 200 but was 409" makes you re-run to learn anything.
     */
    private static void assertOk( Response r ) {
        if ( r.getStatus() != Response.Status.OK.getStatusCode() ) {
            assertThat( r.getStatus() ).as( "response body: %s", r.readEntity( String.class ) )
                    .isEqualTo( Response.Status.OK.getStatusCode() );
        }
    }

    /**
     * A backup must not modify what it backs up.
     * <p>
     * Every audit event on a curatable sets {@code curationDetails.lastUpdated} to the event date
     * ({@code AbstractCuratableDao#updateCurationDetailsFromAuditEvent}, unconditionally, for every event type).
     * So an {@code AnnotationSetEvent} emitted when a snapshot is captured makes the dataset look edited when
     * nothing about it changed.
     * <p>
     * That is not cosmetic. {@code lastUpdated} is the optimistic-concurrency token the curation commit checks
     * ({@code baseline.lastModified} &rarr; 409), so taking a backup would invalidate every in-flight curator
     * draft on that dataset. It also perturbs anything ordering datasets by recency, and the dataset shows as
     * touched in Gemma 1.0, which reads the same database.
     * <p>
     * Observed on gemma2: snapshotting GSE11630 moved its {@code lastUpdated} to 79 ms after the snapshot's
     * {@code createdAt}. The AnnotationSet row already records that a backup was taken, with its own
     * {@code createdAt}, {@code createdBy} and {@code runId} — the audit event is redundant for a capture that
     * changes nothing.
     */
    @Test
    public void testTakingASnapshotDoesNotMarkTheDatasetAsUpdated() {
        // give the dataset a real lastUpdated first: a freshly seeded one has none, and the token only matters
        // for a dataset somebody has curated
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request()
                .put( Entity.json( "{\"curationDetails\":{\"curationNote\":\"establish a baseline token\"}}" ) ) ) {
            assertOk( r );
        }
        Date before = expressionExperimentService.load( ee.getId() ).getCurationDetails().getLastUpdated();
        assertThat( before ).as( "the commit set a lastUpdated to compare against" ).isNotNull();

        takeSnapshot();

        Date after = expressionExperimentService.load( ee.getId() ).getCurationDetails().getLastUpdated();
        assertThat( after ).as( "a snapshot reads; it must not mark the dataset as edited" ).isEqualTo( before );
    }

    /** Take a snapshot of the dataset's current curation and return the new AnnotationSet's id. */
    private long takeSnapshot() {
        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/snapshot" ).request()
                .post( Entity.json( "" ) ) ) {
            assertThat( r.getStatus() ).isIn( Response.Status.OK.getStatusCode(), Response.Status.CREATED.getStatusCode() );
            String json = r.readEntity( String.class );
            assertThat( json ).as( "the server filled the payload in" ).contains( "payloadJson" );
            return Long.parseLong( json.replaceAll( "(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1" ) );
        }
    }

    private ExperimentalDesignValueObject reloadDesign() {
        return expressionExperimentService.getExperimentalDesignValueObject(
                expressionExperimentService.load( ee.getId() ) );
    }

    private static Set<Long> allFvIds( ExperimentalDesignValueObject d ) {
        return d.getExperimentalFactors().stream()
                .flatMap( f -> f.getValues().stream() )
                .map( FactorValueBasicValueObject::getId )
                .collect( Collectors.toSet() );
    }

    private static ExperimentalDesignValueObject.ExperimentalFactorEntry factorById( ExperimentalDesignValueObject d, Long id ) {
        return d.getExperimentalFactors().stream()
                .filter( f -> id.equals( f.getId() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "factor " + id + " is gone from the design" ) );
    }

    private static FactorValueBasicValueObject fvById( ExperimentalDesignValueObject.ExperimentalFactorEntry f, Long id ) {
        return f.getValues().stream()
                .filter( v -> id.equals( v.getId() ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "factor value " + id + " is gone from the factor" ) );
    }

    /** GEO-style short name for each seeded sample, keyed by biomaterial id — the wire addresses samples by GSM. */
    private Map<Long, String> gsmByBioMaterialId() {
        Map<Long, String> byBmId = new HashMap<>();
        for ( BioAssay ba : expressionExperimentService.thawBioAssays( ee ).getBioAssays() ) {
            if ( ba.getSampleUsed() != null && ba.getAccession() != null ) {
                byBmId.put( ba.getSampleUsed().getId(), ba.getAccession().getAccession() );
            }
        }
        return byBmId;
    }

    private String anyGsm() {
        return expressionExperimentService.thawBioAssays( ee ).getBioAssays().iterator().next()
                .getAccession().getAccession();
    }

    private static long countAssigned( ExperimentalDesignValueObject d, Long fvId ) {
        return d.getBioMaterialAssignments().stream()
                .filter( a -> a.getFactorValueIds().contains( fvId ) )
                .count();
    }

    private AnnotationValueObject findAnnotation( String value ) {
        Set<AnnotationValueObject> annotations = expressionExperimentService.getAnnotations( expressionExperimentService.load( ee.getId() ) );
        return annotations.stream().filter( a -> value.equals( a.getValue() ) ).findFirst().orElse( null );
    }

    // ── run provenance: which agent run applied this commit ───────────────────────────────────────────────

    /** Naming a run mints a COMMIT annotation set carrying that run's build, and the report points at it. */
    @Test
    public void testCommitWithRunRefMintsCommitAnnotationSet() {
        String body = "{"
                + "\"basics\":{\"name\":\"renamed by a run\"},"
                + "\"run\":{\"runId\":\"2026-08-18_allbells147\",\"agentName\":\"cell_type\","
                + "\"model\":\"claude-sonnet-5\",\"runSha\":\"4d8fdbc\",\"agentVersion\":\"0.9.1\"}"
                + "}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }

        AnnotationSet set = annotationSetService.findLatestByInvestigation( ee, AnnotationSetRole.COMMIT );
        assertThat( set ).as( "a named run mints a COMMIT row" ).isNotNull();
        assertThat( set.getRunId() ).isEqualTo( "2026-08-18_allbells147" );
        assertThat( set.getAgentName() ).as( "which specialist — 'the agent' is a fleet" ).isEqualTo( "cell_type" );
        assertThat( set.getModel() ).isEqualTo( "claude-sonnet-5" );
        assertThat( set.getRunSha() ).as( "the sha is not redundant with the model" ).isEqualTo( "4d8fdbc" );
        assertThat( set.getAgentVersion() ).isEqualTo( "0.9.1" );
    }

    /** Provenance is sparse on purpose: an ordinary curator commit names no run and mints nothing. */
    @Test
    public void testCommitWithoutRunRefMintsNothing() {
        String body = "{\"basics\":{\"name\":\"renamed by a curator\"}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        assertThat( annotationSetService.countByInvestigation( ee, AnnotationSetRole.COMMIT ) )
                .as( "no run named, no row" ).isZero();
    }

    /**
     * Provenance with no run to hang it off cannot be stored. Accepting it would drop the fields silently while
     * the caller believes it recorded them.
     */
    @Test
    public void testRunProvenanceWithoutRunIdIsRejected() {
        String body = "{\"basics\":{\"name\":\"x\"},\"run\":{\"agentName\":\"cell_type\",\"runSha\":\"4d8fdbc\"}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.BAD_REQUEST.getStatusCode() );
        }
        assertThat( annotationSetService.countByInvestigation( ee, AnnotationSetRole.COMMIT ) ).isZero();
    }

    /** A preflight writes nothing, so it mints no row either — the run reference is only shape-checked. */
    @Test
    public void testPreflightWithRunRefMintsNothing() {
        String body = "{\"basics\":{\"name\":\"dry run\"},\"run\":{\"runId\":\"2026-08-18_dry\"}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/preflight" )
                .request().post( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        assertThat( annotationSetService.countByInvestigation( ee, AnnotationSetRole.COMMIT ) )
                .as( "a dry run leaves no trace" ).isZero();
    }

    /**
     * One run committing the same dataset twice keeps one row. The unique key is
     * (investigation, role, runId), and a resumed run reuses its id by design.
     */
    @Test
    public void testSameRunCommittingTwiceKeepsOneRow() {
        String run = "\"run\":{\"runId\":\"2026-08-18_resumed\",\"agentName\":\"disease\"}";
        for ( String name : new String[] { "first pass", "second pass" } ) {
            String body = "{\"basics\":{\"name\":\"" + name + "\"}," + run + "}";
            try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
                assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
            }
        }
        assertThat( annotationSetService.countByInvestigation( ee, AnnotationSetRole.COMMIT ) )
                .as( "a resumed run is one run" ).isEqualTo( 1 );
    }

    /**
     * A no-op commit that named a run still mints. Absence has to mean "no run was named", never "the run did
     * nothing" — those are different facts and would otherwise have identical bytes.
     */
    @Test
    public void testNoOpCommitNamingARunStillMints() {
        String currentName = expressionExperimentService.load( ee.getId() ).getName();
        String body = "{\"basics\":{\"name\":\"" + currentName + "\"},"
                + "\"run\":{\"runId\":\"2026-08-18_noop\"}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        assertThat( annotationSetService.countByInvestigation( ee, AnnotationSetRole.COMMIT ) )
                .as( "the run ran, and said so, even though nothing changed" ).isEqualTo( 1 );
    }

    /**
     * A COMMIT may only name a PROPOSAL as the thing it applied. Proposed-versus-applied is the distinction the
     * provenance surface rests on, so a DRAFT or SNAPSHOT in that slot is a client bug, not a silent no-op.
     */
    @Test
    public void testCommitNamingANonProposalParentIsRejected() {
        AnnotationSet snapshot = annotationSetService.attach( ee, AnnotationSetRole.SNAPSHOT,
                ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSource.AGENT, null,
                null, "tester", null, null, null ).getAnnotationSet();

        String body = "{\"basics\":{\"name\":\"x\"},\"run\":{\"runId\":\"r1\",\"proposalSetId\":"
                + snapshot.getId() + "}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.BAD_REQUEST.getStatusCode() );
        }
        assertThat( annotationSetService.countByInvestigation( ee, AnnotationSetRole.COMMIT ) ).isZero();
    }


    /**
     * Naming a run must not cost an extra audit event.
     * <p>
     * Every audit event on a curatable sets {@code curationDetails.lastUpdated}
     * ({@code AbstractCuratableDao#updateCurationDetailsFromAuditEvent}, unconditionally), and that is the
     * optimistic-concurrency token the next commit checks. One commit already emits several events; an
     * {@code AnnotationSetEvent} for the COMMIT row on top would be one more, saying nothing the section events
     * did not — so {@code AnnotationSetServiceImpl#ATTACH_AUDIT_WHEN} suppresses it for COMMIT as it does for
     * SNAPSHOT. This pins that: the same commit costs the same events whether or not it names a run.
     */
    @Test
    public void testNamingARunCostsNoExtraAuditEvent() {
        int before = auditEventService.getEvents( expressionExperimentService.load( ee.getId() ) ).size();
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request()
                .put( Entity.json( "{\"basics\":{\"name\":\"unstamped\"}}" ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        int afterUnstamped = auditEventService.getEvents( expressionExperimentService.load( ee.getId() ) ).size();
        int costOfAPlainCommit = afterUnstamped - before;

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request()
                .put( Entity.json( "{\"basics\":{\"name\":\"stamped\"},"
                        + "\"run\":{\"runId\":\"2026-08-18_quiet\",\"agentName\":\"cell_type\"}}" ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        int afterStamped = auditEventService.getEvents( expressionExperimentService.load( ee.getId() ) ).size();

        assertThat( annotationSetService.countByInvestigation( ee, AnnotationSetRole.COMMIT ) )
                .as( "the run really was recorded" ).isEqualTo( 1 );
        assertThat( afterStamped - afterUnstamped )
                .as( "stamping a commit with its run is free in audit events" )
                .isEqualTo( costOfAPlainCommit );
    }


    /**
     * A preflight must not audit. cab describes their permitted set under the Gemma 1.0 hold as "reads and
     * preflight only", and that phrase is only true if a dry run emits nothing: every audit event on a curatable
     * sets {@code curationDetails.lastUpdated}, and {@code AnnotationSetEvent} / {@code DesignChangeEvent} are
     * among the 21 discriminators Gemma 1.0 cannot load (PR #1667) — so an auditing preflight would both move the
     * concurrency token and break the 1.0 experiment page.
     * <p>
     * Verified by reading the code once ({@code previewDesignChange} is {@code @Transactional(readOnly = true)}
     * with no audit call in 300 lines); pinned here so it stays true.
     */
    @Test
    public void testPreflightEmitsNoAuditEvent() {
        ExpressionExperiment thawed = expressionExperimentService.thawBioAssays( ee );
        BioAssay ba = thawed.getBioAssays().iterator().next();
        String gsm = ba.getAccession().getAccession();

        int before = auditEventService.getEvents( expressionExperimentService.load( ee.getId() ) ).size();

        // a preflight over every section a commit can touch, including a design change and a tag
        String body = "{"
                + "\"basics\":{\"name\":\"preflighted\"},"
                + "\"tags\":{\"items\":[{\"clientRef\":\"t1\",\"category\":{\"label\":\"organism part\"},"
                + "\"value\":{\"label\":\"liver\"}}]},"
                + "\"design\":{\"factors\":{\"items\":[{\"clientRef\":\"f1\",\"name\":\"preflight factor\","
                + "\"type\":\"CATEGORICAL\",\"category\":{\"label\":\"treatment\"},"
                + "\"factorValues\":{\"items\":[{\"clientRef\":\"fv1\",\"freeTextLabel\":\"treated\","
                + "\"biomaterialShortNames\":[\"" + gsm + "\"]}]}}]}},"
                + "\"curationDetails\":{\"curationNote\":\"a note that must not land\"},"
                + "\"run\":{\"runId\":\"2026-08-18_preflight\"}"
                + "}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/preflight" )
                .request().post( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }

        int after = auditEventService.getEvents( expressionExperimentService.load( ee.getId() ) ).size();
        assertThat( after ).as( "a preflight reads; it must emit no audit event" ).isEqualTo( before );
        assertThat( annotationSetService.countByInvestigation( ee, AnnotationSetRole.COMMIT ) )
                .as( "and mints no annotation set" ).isZero();
    }


    // ── the commit keeps what it displaced ───────────────────────────────────────────────────────────────

    /**
     * The restore point nobody had to remember to ask for. A commit that changes anything first stores the
     * curation it is about to overwrite as a SNAPSHOT, and the id it reports feeds the ordinary restore — so the
     * undo exists even though the curator took no backup beforehand.
     */
    @Test
    public void testCommitKeepsWhatItDisplacedAndTheReportPointsAtIt() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = before.getExperimentalFactors().stream()
                .filter( f -> !f.getValues().isEmpty() )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seeded design has no factor with values" ) );
        FactorValueBasicValueObject target = factor.getValues().get( 0 );
        String originalSummary = target.getSummary();

        long snapshotId;
        String edit = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":" + factor.getId() + ","
                + "\"factorValues\":{\"items\":[{\"gemmaId\":" + target.getId() + ","
                + "\"freeTextLabel\":\"committed over\"}]}}]}}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( edit ) ) ) {
            assertOk( r );
            String json = r.readEntity( String.class );
            assertThat( json ).as( "the commit says where the curation it displaced went" )
                    .containsPattern( "\"snapshotAnnotationSetId\"\\s*:\\s*\\d+" );
            snapshotId = Long.parseLong( json.replaceAll( "(?s).*\"snapshotAnnotationSetId\"\\s*:\\s*(\\d+).*", "$1" ) );
        }
        assertThat( fvById( factorById( reloadDesign(), factor.getId() ), target.getId() ).getValue() )
                .isEqualTo( "committed over" );

        AnnotationSet kept = annotationSetService.load( snapshotId );
        assertThat( kept ).as( "the reported row exists" ).isNotNull();
        assertThat( kept.getRole() ).isEqualTo( AnnotationSetRole.SNAPSHOT );
        assertThat( kept.getRunId() ).as( "a commit-taken backup is distinguishable from one somebody asked for" )
                .startsWith( AnnotationSetService.PRE_COMMIT_SNAPSHOT_RUN_ID_PREFIX );

        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/" + snapshotId + "/restore" )
                .queryParam( "force", true ).request().post( Entity.json( "" ) ) ) {
            assertOk( r );
        }
        assertThat( fvById( factorById( reloadDesign(), factor.getId() ), target.getId() ).getSummary() )
                .as( "the commit is undone from the backup it took itself" ).isEqualTo( originalSummary );
    }

    /**
     * A commit that changes nothing displaced nothing, so it keeps nothing. A row per retry would bury the
     * restore points that matter under identical copies of a state nobody overwrote.
     */
    @Test
    public void testANoOpCommitKeepsNoSnapshot() {
        String currentName = expressionExperimentService.load( ee.getId() ).getName();
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request()
                .put( Entity.json( "{\"basics\":{\"name\":\"" + currentName + "\"}}" ) ) ) {
            assertOk( r );
        }
        assertThat( annotationSetService.countByInvestigation( ee, AnnotationSetRole.SNAPSHOT ) )
                .as( "nothing was displaced, so there is nothing to keep" ).isZero();
    }

    /** A preflight writes nothing, so there is nothing to keep and no row left behind to explain. */
    @Test
    public void testPreflightKeepsNoSnapshot() {
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/preflight" ).request()
                .post( Entity.json( "{\"basics\":{\"name\":\"dry run\"},"
                        + "\"curationDetails\":{\"curationNote\":\"not landing\"}}" ) ) ) {
            assertOk( r );
        }
        assertThat( annotationSetService.countByInvestigation( ee, AnnotationSetRole.SNAPSHOT ) )
                .as( "a dry run leaves no trace" ).isZero();
    }

    /**
     * Keeping the displaced curation has to be free in audit events. Every audit event on a curatable sets
     * {@code curationDetails.lastUpdated} — the optimistic-concurrency token the next commit checks — so a
     * backup that audited would 409 in-flight drafts by the act of backing up. A basics-only commit emits no
     * event of its own, which is what makes the count here a direct reading of the backup's cost.
     */
    @Test
    public void testKeepingTheDisplacedCurationCostsNoAuditEvent() {
        int before = auditEventService.getEvents( expressionExperimentService.load( ee.getId() ) ).size();
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request()
                .put( Entity.json( "{\"basics\":{\"name\":\"renamed, and backed up\"}}" ) ) ) {
            assertOk( r );
        }
        int after = auditEventService.getEvents( expressionExperimentService.load( ee.getId() ) ).size();

        assertThat( annotationSetService.countByInvestigation( ee, AnnotationSetRole.SNAPSHOT ) )
                .as( "the backup really was taken" ).isEqualTo( 1 );
        assertThat( after ).as( "and it cost no audit event" ).isEqualTo( before );
    }

    /**
     * The evidence a commit is given is recorded, not dropped. {@code PublicationEntry} carries the basis for a
     * link on every endpoint that accepts one, and a commit that quietly kept only the identifier would leave the
     * caller believing it had recorded a reason it never stored.
     */
    @Test
    public void testCommitRecordsTheBasisGivenForAPublication() {
        // seeded locally so resolving the id is a database lookup, not a PubMed fetch
        testHelper.getTestPersistentBibliographicReference( "20051063" );

        String attach = "{\"publications\":{\"primary\":{\"pubMedId\":\"20051063\","
                + "\"source\":\"geo_submitter_link\",\"evidence\":\"the series links this paper\","
                + "\"evidenceCode\":\"TAS\"},\"otherRelevant\":[]}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( attach ) ) ) {
            assertOk( r );
        }

        try ( Response r = target( "/datasets/" + ee.getId() + "/publications" ).request().get() ) {
            assertOk( r );
            String json = r.readEntity( String.class );
            assertThat( json ).as( "the paper is attached" ).contains( "20051063" );
            assertThat( json ).as( "with the basis given for it" ).contains( "the series links this paper" );
            assertThat( json ).as( "and the claimed source, not a curator claim invented for it" )
                    .contains( "geo_submitter_link" );
        }
    }

    /**
     * A commit that drops a paper is undone from its own backup, and the paper comes back with the basis it had.
     * The basis is how a later reader judges the link — a restore that returns the fact without it hands back
     * something weaker than what was taken away.
     */
    @Test
    public void testRestoreBringsBackADroppedPublicationWithItsBasis() {
        testHelper.getTestPersistentBibliographicReference( "20051063" );

        String attach = "{\"publications\":{\"primary\":{\"pubMedId\":\"20051063\","
                + "\"source\":\"geo_submitter_link\",\"evidence\":\"the series links this paper\","
                + "\"evidenceCode\":\"TAS\"},\"otherRelevant\":[]}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( attach ) ) ) {
            assertOk( r );
        }

        // the curator drops it — and the commit keeps what it displaced
        long snapshotId;
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request()
                .put( Entity.json( "{\"publications\":{\"primary\":null,\"otherRelevant\":[]}}" ) ) ) {
            assertOk( r );
            String json = r.readEntity( String.class );
            assertThat( json ).containsPattern( "\"snapshotAnnotationSetId\"\\s*:\\s*\\d+" );
            snapshotId = Long.parseLong( json.replaceAll( "(?s).*\"snapshotAnnotationSetId\"\\s*:\\s*(\\d+).*", "$1" ) );
        }
        try ( Response r = target( "/datasets/" + ee.getId() + "/publications" ).request().get() ) {
            assertThat( r.readEntity( String.class ) ).as( "the paper really is gone" ).doesNotContain( "20051063" );
        }

        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/" + snapshotId + "/restore" )
                .queryParam( "force", true ).request().post( Entity.json( "" ) ) ) {
            assertOk( r );
        }

        try ( Response r = target( "/datasets/" + ee.getId() + "/publications" ).request().get() ) {
            assertOk( r );
            String json = r.readEntity( String.class );
            assertThat( json ).as( "the paper is back" ).contains( "20051063" );
            assertThat( json ).as( "with the basis it had" ).contains( "the series links this paper" );
            assertThat( json ).as( "and its source, not rewritten as a curator claim by the restore" )
                    .contains( "geo_submitter_link" );
        }
    }

    /**
     * A client that has just committed knows the state — its own write produced it — so it should be able to keep
     * editing and commit again. The report hands back the token for that. Without it the only way to learn the new
     * baseline is to re-read the dataset, and a client that skips the re-read 409s on a change it made itself.
     */
    @Test
    public void testTheReportHandsBackTheTokenForTheNextCommit() {
        String token;
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request()
                .put( Entity.json( "{\"basics\":{\"name\":\"first\"}}" ) ) ) {
            assertOk( r );
            String json = r.readEntity( String.class );
            assertThat( json ).as( "the report names the next commit's baseline" )
                    .containsPattern( "\"newBaseline\"\\s*:\\s*\"[^\"]+\"" );
            token = json.replaceAll( "(?s).*\"newBaseline\"\\s*:\\s*\"([^\"]+)\".*", "$1" );
        }

        // second commit carrying that token, with no re-read in between
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request()
                .put( Entity.json( "{\"baseline\":{\"lastModified\":\"" + token + "\"},"
                        + "\"basics\":{\"name\":\"second\"}}" ) ) ) {
            assertOk( r );
        }

        // the same token a third time is refused — which is what proves it was parsed and compared rather than
        // quietly ignored, since an unreadable token skips the check and lets the commit through
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request()
                .put( Entity.json( "{\"baseline\":{\"lastModified\":\"" + token + "\"},"
                        + "\"basics\":{\"name\":\"third\"}}" ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.CONFLICT.getStatusCode() );
            assertThat( r.readEntity( String.class ) ).as( "named as the conflict it is" )
                    .contains( "\"reason\":\"STALE_BASELINE\"" );
        }
        assertThat( expressionExperimentService.load( ee.getId() ).getName() )
                .as( "the refused commit wrote nothing" ).isEqualTo( "second" );
    }

    /* ============== draft delegation: the agent writes for a curator ============== */

    /**
     * The bug this gate exists for. A DRAFT's run id is {@code "draft-{curator}"} and that run id sits inside
     * {@code UNIQUE(investigation, role, runId)} — so when the agent relays two curators' drafts without naming
     * them, both key to the agent's own identity, become one row, and the second autosave overwrites the first
     * with no error at any layer. Asserted as two surviving rows with the right owners rather than as a status
     * code, because the failure mode is a successful-looking 200.
     */
    @Test
    public void testAgentDraftsForTwoCuratorsDoNotCollapseOntoOneRow() {
        testAuthenticationUtils.runAsUser( "draftalice", true );
        testAuthenticationUtils.runAsUser( "draftbob", true );
        testAuthenticationUtils.runAsAgent();

        putDraftAs( "draftalice", "{\"factor:1\":{\"name\":\"alice's edit\"}}" );
        putDraftAs( "draftbob", "{\"factor:1\":{\"name\":\"bob's edit\"}}" );

        testAuthenticationUtils.runAsAdmin();
        List<AnnotationSet> drafts = annotationSetService.findByInvestigation( ee, AnnotationSetRole.DRAFT );
        assertThat( drafts ).as( "one draft per curator, not one shared row" ).hasSize( 2 );
        assertThat( drafts ).extracting( AnnotationSet::getCreatedBy )
                .containsExactlyInAnyOrder( "draftalice", "draftbob" );
        assertThat( drafts ).extracting( AnnotationSet::getPayloadJson )
                .anyMatch( j -> j.contains( "alice's edit" ) )
                .anyMatch( j -> j.contains( "bob's edit" ) );
    }

    /**
     * Reading is delegated for the same reason writing is: an agent that asks for "the draft" without naming a
     * curator gets its own, which is never what it means.
     */
    @Test
    public void testAgentReadsTheNamedCuratorsDraft() {
        testAuthenticationUtils.runAsUser( "draftcarol", true );
        testAuthenticationUtils.runAsAgent();
        putDraftAs( "draftcarol", "{\"factor:1\":{\"name\":\"carol's edit\"}}" );

        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/draft" )
                .queryParam( "onBehalfOf", "draftcarol" ).request().get() ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) ).contains( "carol's edit" ).contains( "draftcarol" );
        }
        // and without the parameter there is nothing of the agent's own to find
        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/draft" ).request().get() ) {
            assertThat( r.getStatus() ).as( "the agent has no draft of its own" )
                    .isEqualTo( Response.Status.NOT_FOUND.getStatusCode() );
        }
        testAuthenticationUtils.runAsAdmin();
    }

    /**
     * A plain curator claiming another identity is refused rather than silently rewritten to their own — storing
     * a different fact from the one they asked for is worse than declining.
     */
    @Test
    public void testAPlainCuratorMayNotWriteAsSomeoneElse() {
        testAuthenticationUtils.runAsUser( "drafteve", true );
        testAuthenticationUtils.runAsUser( "draftmallory", true );
        try {
            testAuthenticationUtils.runAsUser( "draftmallory", false );
            try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/draft" )
                    .queryParam( "onBehalfOf", "drafteve" )
                    .request().put( Entity.json( "{\"payloadJson\":\"{}\"}" ) ) ) {
                assertThat( r.getStatus() ).as( "delegation is refused, not ignored" )
                        .isNotEqualTo( Response.Status.OK.getStatusCode() )
                        .isNotEqualTo( Response.Status.CREATED.getStatusCode() );
            }
        } finally {
            testAuthenticationUtils.runAsAdmin();
        }
        assertThat( annotationSetService.findByInvestigation( ee, AnnotationSetRole.DRAFT ) )
                .as( "and nothing was written under either name" ).isEmpty();
    }

    private void putDraftAs( String curator, String payloadJson ) {
        String body = "{\"payloadJson\":" + quoteJson( payloadJson ) + "}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets/draft" )
                .queryParam( "onBehalfOf", curator ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).as( "response body: %s", r.readEntity( String.class ) )
                    .isIn( Response.Status.OK.getStatusCode(), Response.Status.CREATED.getStatusCode() );
        }
    }

    /** Embed a JSON document as a JSON string value. */
    private static String quoteJson( String raw ) {
        return "\"" + raw.replace( "\\", "\\\\" ).replace( "\"", "\\\"" ) + "\"";
    }


    /* ============== triage ============== */

    /**
     * The agent's own verdict and a curator's relayed one coexist on the same set, and the curator's is recorded
     * as CURATOR rather than as the agent that transmitted it. If the kind followed the transport instead of the
     * delegation, "has a person looked at this" would answer false for every ruling a curator ever made -- which,
     * now that curation is relayed, is all of them.
     */
    @Test
    public void testAgentAndCuratorVerdictsCoexistWithTheRightKinds() {
        Long setId = mintProposal( "triage-run-1" );
        testAuthenticationUtils.runAsUser( "triagealice", true );
        testAuthenticationUtils.runAsAgent();

        patchTriage( setId, null, "{\"triage\":\"must_fix\"}" );
        patchTriage( setId, "triagealice", "{\"triage\":\"wont_fix\",\"note\":\"batch artifact\"}" );

        try ( Response r = target( "/annotation-sets/" + setId + "/triage" ).request().get() ) {
            assertOk( r );
            String json = r.readEntity( String.class );
            assertThat( json ).contains( "must_fix" ).contains( "wont_fix" )
                    .contains( "\"judgeKind\":\"agent\"" ).contains( "\"judgeKind\":\"curator\"" )
                    .contains( "triagealice" ).contains( "batch artifact" );
        }
        testAuthenticationUtils.runAsAdmin();
    }

    /**
     * A judge has one standing ruling, so changing your mind edits rather than accumulates.
     */
    @Test
    public void testSameJudgeRulingTwiceLeavesOneRow() {
        Long setId = mintProposal( "triage-run-2" );
        testAuthenticationUtils.runAsUser( "triagebob", true );
        testAuthenticationUtils.runAsAgent();

        patchTriage( setId, "triagebob", "{\"triage\":\"might_fix\"}" );
        patchTriage( setId, "triagebob", "{\"triage\":\"fine\"}" );

        try ( Response r = target( "/annotation-sets/" + setId + "/triage" ).request().get() ) {
            assertOk( r );
            String json = r.readEntity( String.class );
            assertThat( json ).contains( "fine" ).doesNotContain( "might_fix" );
        }
        testAuthenticationUtils.runAsAdmin();
    }

    /**
     * There is no `pending` verdict -- an un-ruled set has no row -- so a client sending one is told rather than
     * silently given a verdict nobody chose.
     */
    @Test
    public void testPendingIsNotAVerdict() {
        Long setId = mintProposal( "triage-run-3" );
        try ( Response r = target( "/annotation-sets/" + setId + "/triage" )
                .request().method( "PATCH", Entity.json( "{\"triage\":\"pending\"}" ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.BAD_REQUEST.getStatusCode() );
        }
        try ( Response r = target( "/annotation-sets/" + setId + "/triage" ).request().get() ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) ).as( "still un-triaged" ).isEqualTo( "[]" );
        }
    }

    /**
     * Withdrawing returns the set to un-triaged, and withdrawing again is not an error -- a withdrawal that finds
     * nothing has still achieved what it asked for.
     */
    @Test
    public void testWithdrawReturnsTheSetToUntriaged() {
        Long setId = mintProposal( "triage-run-4" );
        patchTriage( setId, null, "{\"triage\":\"fine\"}" );
        for ( int i = 0; i < 2; i++ ) {
            try ( Response r = target( "/annotation-sets/" + setId + "/triage" ).request().delete() ) {
                assertThat( r.getStatus() ).isEqualTo( Response.Status.NO_CONTENT.getStatusCode() );
            }
        }
        try ( Response r = target( "/annotation-sets/" + setId + "/triage" ).request().get() ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) ).isEqualTo( "[]" );
        }
    }

    /**
     * Create a PROPOSAL on the seeded experiment and return its id, read off the response body the way
     * testRestoringANonSnapshotIsRejected does -- the entity is consumed once, so the id has to come out of the
     * same read that checks the status.
     */
    private Long mintProposal( String runId ) {
        String body = "{\"role\":\"proposal\",\"source\":\"agent\",\"runId\":\"" + runId
                + "\",\"createdBy\":\"agent-1\",\"payloadJson\":\"{}\"}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/annotation-sets" )
                .request().post( Entity.json( body ) ) ) {
            String json = r.readEntity( String.class );
            assertThat( r.getStatus() ).as( "response body: %s", json )
                    .isIn( Response.Status.OK.getStatusCode(), Response.Status.CREATED.getStatusCode() );
            return Long.parseLong( json.replaceAll( "(?s).*?\"id\"\\s*:\\s*(\\d+).*", "$1" ) );
        }
    }

    private void patchTriage( Long setId, @Nullable String onBehalfOf, String body ) {
        jakarta.ws.rs.client.WebTarget t = target( "/annotation-sets/" + setId + "/triage" );
        if ( onBehalfOf != null ) {
            t = t.queryParam( "onBehalfOf", onBehalfOf );
        }
        try ( Response r = t.request().method( "PATCH", Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).as( "response body: %s", r.readEntity( String.class ) )
                    .isEqualTo( Response.Status.OK.getStatusCode() );
        }
    }


    /* ============== curation lock ============== */

    @Test
    public void testLockIsFreeThenHeldThenReleased() {
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" ).request().get() ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) ).contains( "\"locked\":false" );
        }
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" )
                .request().post( Entity.json( "" ) ) ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) ).contains( "\"locked\":true" ).contains( "expiresAt" );
        }
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" ).request().delete() ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.NO_CONTENT.getStatusCode() );
        }
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" ).request().get() ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) ).contains( "\"locked\":false" );
        }
    }

    /**
     * A second curator is refused with a 409 that NAMES the holder, then gets it with ?steal=true. "Someone else
     * has it" without saying who leaves the curator with nobody to ask, which is why the holder is in the message
     * rather than only in the GET.
     */
    @Test
    public void testSecondCuratorIsRefusedByNameThenMaySteal() {
        testAuthenticationUtils.runAsAgent();
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" )
                .queryParam( "onBehalfOf", "lockalice" ).request().post( Entity.json( "" ) ) ) {
            assertOk( r );
        }
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" )
                .queryParam( "onBehalfOf", "lockbob" ).request().post( Entity.json( "" ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.CONFLICT.getStatusCode() );
            assertThat( r.readEntity( String.class ) ).contains( "lockalice" ).contains( "steal" );
        }
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" )
                .queryParam( "onBehalfOf", "lockbob" ).queryParam( "steal", true )
                .request().post( Entity.json( "" ) ) ) {
            assertOk( r );
            String json = r.readEntity( String.class );
            assertThat( json ).contains( "lockbob" ).contains( "\"stolenFrom\":\"lockalice\"" );
        }
        testAuthenticationUtils.runAsAdmin();
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" ).request().delete() ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.NO_CONTENT.getStatusCode() );
        }
    }


    /**
     * The agent authenticates as whichever account runs it, which today is a human administrator. Inferring the
     * judge kind from the transport therefore reports CURATOR for the agent's own verdicts, and
     * reviewedByHuman then answers true for rulings no person made -- the exact failure JUDGE_KIND exists to
     * prevent, inverted. So the caller declares it.
     */
    @Test
    public void testAgentRunningAsAdminStillRecordsItsOwnVerdictAsAgent() {
        Long setId = mintProposal( "triage-run-kind" );
        // no onBehalfOf, and the caller is administrator -- not GROUP_AGENT
        patchTriage( setId, null, "{\"triage\":\"must_fix\",\"judgeKind\":\"agent\"}" );

        try ( Response r = target( "/annotation-sets/" + setId + "/triage" ).request().get() ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) )
                    .as( "a declared agent verdict is not relabelled by the transport identity" )
                    .contains( "\"judgeKind\":\"agent\"" );
        }
    }

    /** An unknown judgeKind is refused rather than silently defaulted to a judge nobody named. */
    @Test
    public void testUnknownJudgeKindIsRejected() {
        Long setId = mintProposal( "triage-run-kind-bad" );
        try ( Response r = target( "/annotation-sets/" + setId + "/triage" )
                .request().method( "PATCH", Entity.json( "{\"triage\":\"fine\",\"judgeKind\":\"robot\"}" ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.BAD_REQUEST.getStatusCode() );
        }
    }


    /**
     * Editing holds the lease. Without the refresh on the draft save, a curator working steadily for longer
     * than the TTL loses the lock while still typing -- and finds out only when someone else takes it.
     * Asserted as a moved expiry rather than by waiting out a real TTL.
     */
    @Test
    public void testSavingADraftExtendsTheCuratorsLock() {
        testAuthenticationUtils.runAsAgent();
        String expiryAfterAcquire;
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" )
                .queryParam( "onBehalfOf", "leasealice" ).queryParam( "ttlMinutes", 1 )
                .request().post( Entity.json( "" ) ) ) {
            assertOk( r );
            expiryAfterAcquire = r.readEntity( String.class ).replaceAll( "(?s).*\"expiresAt\":\"([^\"]+)\".*", "$1" );
        }
        putDraftAs( "leasealice", "{\"factor:1\":{\"name\":\"still editing\"}}" );

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" ).request().get() ) {
            assertOk( r );
            String json = r.readEntity( String.class );
            String now = json.replaceAll( "(?s).*\"expiresAt\":\"([^\"]+)\".*", "$1" );
            assertThat( json ).contains( "leasealice" );
            // the save pushed the lease out to the default TTL, well past the 1-minute one it was acquired with
            assertThat( now ).isGreaterThan( expiryAfterAcquire );
        }
        testAuthenticationUtils.runAsAdmin();
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" ).request().delete() ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.NO_CONTENT.getStatusCode() );
        }
    }

    /** A save by someone who holds no lock takes none -- a refresh must never become an acquire. */
    @Test
    public void testSavingADraftDoesNotTakeALockYouDoNotHold() {
        testAuthenticationUtils.runAsAgent();
        putDraftAs( "leasebob", "{\"factor:1\":{\"name\":\"no lock held\"}}" );
        testAuthenticationUtils.runAsAdmin();
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" ).request().get() ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) ).contains( "\"locked\":false" );
        }
    }

    // ============================================================================================
    // Sign-off — POST /datasets/{id}/curation/sign
    //
    // An ordinary commit refuses a change that destroys derived data (409 REQUIRES_FORCE). Sign is
    // where such a change belongs, and what earns it is the curation lock. These tests pin both halves:
    // the gate holds, and past the gate the change applies.
    //
    // 🛑 The lock is no longer sign-only. It now refuses ANY write by a non-holder while someone else
    // holds it — see the commit-gate tests at the bottom of this class.
    //
    // The seeded experiment carries no differential-expression analyses, so requiresForce() is made
    // true here by stranding a subset — the other half of the same predicate.
    // ============================================================================================

    /** The whole gate: no lock, no sign, nothing written. */
    @Test
    public void testSignWithoutTheLockIsRefused() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = factorWithAnAssignedValue( before );
        FactorValueBasicValueObject anchor = firstAssignedValue( before, factor );
        ExpressionExperimentSubSet subset = anchorSubsetOn( before, anchor );

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/sign" ).request()
                .post( Entity.json( deleteFactorValue( factor.getId(), anchor.getId() ) ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.CONFLICT.getStatusCode() );
            assertThat( r.readEntity( String.class ) )
                    .as( "the client is told which 409 this is, not left to read the sentence" )
                    .contains( "\"reason\":\"LOCK_REQUIRED\"" );
        }
        assertThat( allFvIds( reloadDesign() ) ).as( "and nothing was written" ).contains( anchor.getId() );

        expressionExperimentSubSetService.remove( subset );
    }

    /**
     * Someone else's lock is not yours. The refusal names the holder, as the lock endpoint's own 409 does:
     * "you cannot sign" without saying who is in the way leaves the curator with nobody to ask.
     */
    @Test
    public void testSignWhileAnotherCuratorHoldsTheLockIsRefused() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = factorWithAnAssignedValue( before );
        FactorValueBasicValueObject anchor = firstAssignedValue( before, factor );
        ExpressionExperimentSubSet subset = anchorSubsetOn( before, anchor );

        testAuthenticationUtils.runAsAgent();
        takeLockFor( "signalice" );
        testAuthenticationUtils.runAsAdmin();

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/sign" ).request()
                .post( Entity.json( deleteFactorValue( factor.getId(), anchor.getId() ) ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.CONFLICT.getStatusCode() );
            String json = r.readEntity( String.class );
            assertThat( json ).contains( "\"reason\":\"LOCK_REQUIRED\"" );
            assertThat( json ).as( "and it names who holds it" ).contains( "signalice" );
        }
        assertThat( allFvIds( reloadDesign() ) ).contains( anchor.getId() );

        expressionExperimentSubSetService.remove( subset );
    }

    /**
     * The point of the endpoint. The same payload that a plain commit refuses goes through sign once the lock
     * is held — with no {@code ?force=true} anywhere, because the signature is the consent.
     */
    @Test
    public void testSignAppliesWhatAPlainCommitRefuses() {
        ExperimentalDesignValueObject before = expressionExperimentService.getExperimentalDesignValueObject( ee );
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = factorWithAnAssignedValue( before );
        FactorValueBasicValueObject anchor = firstAssignedValue( before, factor );
        ExpressionExperimentSubSet subset = anchorSubsetOn( before, anchor );
        String body = deleteFactorValue( factor.getId(), anchor.getId() );

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).as( "the plain commit still refuses it" )
                    .isEqualTo( Response.Status.CONFLICT.getStatusCode() );
            assertThat( r.readEntity( String.class ) ).contains( "\"reason\":\"REQUIRES_FORCE\"" );
        }
        assertThat( allFvIds( reloadDesign() ) ).contains( anchor.getId() );

        takeLockFor( null );
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/sign" ).request()
                .post( Entity.json( body ) ) ) {
            assertOk( r );
        }
        assertThat( allFvIds( reloadDesign() ) ).as( "and past the lock it goes through" )
                .doesNotContain( anchor.getId() );
        assertThat( lockState() ).as( "signing off ends the turn, so the lock goes back" )
                .contains( "\"locked\":false" );

        expressionExperimentSubSetService.remove( subset );
    }

    /** With no body, what gets signed is the caller's own draft — the held-back delta. */
    @Test
    public void testSignWithNoBodySignsTheDraft() {
        putDraftAs( "administrator", "{\"curationDetails\":{\"curationNote\":\"signed off from the draft\"}}" );
        takeLockFor( null );

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/sign" ).request()
                .post( Entity.json( "{}" ) ) ) {
            assertOk( r );
        }
        assertThat( expressionExperimentService.load( ee.getId() ).getCurationDetails().getCurationNote() )
                .isEqualTo( "signed off from the draft" );
    }

    /** ... and with neither a body nor a draft, there is nothing to sign. Say so rather than reporting success. */
    @Test
    public void testSignWithNoBodyAndNoDraftIsRejected() {
        takeLockFor( null );
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/sign" ).request()
                .post( Entity.json( "{}" ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.BAD_REQUEST.getStatusCode() );
            assertThat( r.readEntity( String.class ) ).contains( "no draft" );
        }
    }

    /**
     * A sign that fails keeps the lock. The curator's next move is to re-read and sign again, and taking their
     * lock away mid-refusal would make them re-acquire it — or find someone else had.
     */
    @Test
    public void testAFailedSignKeepsTheLock() {
        takeLockFor( null );
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/sign" ).request()
                .post( Entity.json( "{\"curationDetails\":{\"troubled\":true}}" ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.BAD_REQUEST.getStatusCode() );
        }
        assertThat( lockState() ).contains( "\"locked\":true" );
    }

    /** A dry run predicts; it must not release anything. */
    @Test
    public void testADryRunSignDoesNotReleaseTheLock() {
        putDraftAs( "administrator", "{\"curationDetails\":{\"curationNote\":\"not signed yet\"}}" );
        takeLockFor( null );

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/sign" ).queryParam( "dryRun", true )
                .request().post( Entity.json( "{}" ) ) ) {
            assertOk( r );
        }
        assertThat( lockState() ).contains( "\"locked\":true" );
        assertThat( expressionExperimentService.load( ee.getId() ).getCurationDetails().getCurationNote() )
                .as( "and it wrote nothing" ).isNotEqualTo( "not signed yet" );
    }

    /** The lock endpoint's own answer, as a string to assert against. */
    private String lockState() {
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" ).request().get() ) {
            assertOk( r );
            return r.readEntity( String.class );
        }
    }

    /** Take the curation lock, for the caller or for a named curator. */
    private void takeLockFor( @Nullable String onBehalfOf ) {
        jakarta.ws.rs.client.WebTarget t = target( "/datasets/" + ee.getId() + "/curation/lock" );
        if ( onBehalfOf != null ) {
            t = t.queryParam( "onBehalfOf", onBehalfOf );
        }
        try ( Response r = t.request().post( Entity.json( "{}" ) ) ) {
            assertOk( r );
        }
    }

    /** A CurationDocument that deletes one factor value from one factor, leaving the factor in place. */
    private static String deleteFactorValue( Long factorId, Long fvId ) {
        return "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":" + factorId + ","
                + "\"factorValues\":{\"items\":[],\"deletedIds\":[" + fvId + "]}}]}}}";
    }

    // ============================================================================================
    // The commit gate — holding the lock refuses other people's writes
    //
    // The lock used to be advisory everywhere except sign-off: "nothing downstream may read a held
    // lock as permission", with baseline.lastModified as the correctness guarantee. It still is the
    // correctness guarantee. What changed is that a batch run can state its claim BEFORE the work
    // rather than discovering each collision after doing that dataset's work.
    //
    // The non-breaking half matters as much as the gate: an unheld dataset must commit exactly as it
    // always did, or every existing client starts 409ing the day this ships.
    // ============================================================================================

    /** The gate: someone else holds it, so the write is refused and names them. */
    @Test
    public void testCommitWhileAnotherCuratorHoldsTheLockIsRefused() {
        testAuthenticationUtils.runAsAgent();
        takeLockFor( "batchalice" );
        testAuthenticationUtils.runAsAdmin();

        String body = "{\"curationDetails\":{\"curationNote\":\"side edit\"}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.CONFLICT.getStatusCode() );
            String json = r.readEntity( String.class );
            assertThat( json ).contains( "\"reason\":\"LOCK_REQUIRED\"" );
            assertThat( json ).as( "and it names who is in the way" ).contains( "batchalice" );
        }
    }

    /**
     * 🛑 The non-breaking guarantee. Nobody holds the lock, so the commit behaves exactly as it did
     * before the gate existed. If this ever fails, every client that commits without acquiring —
     * which today is all of them — is broken.
     */
    @Test
    public void testCommitOnAnUnheldDatasetIsNotGated() {
        String body = "{\"curationDetails\":{\"curationNote\":\"no lock anywhere\"}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertOk( r );
        }
    }

    /** Holding it yourself is not being blocked by it. */
    @Test
    public void testCommitByTheLockHolderIsNotGated() {
        takeLockFor( null );
        String body = "{\"curationDetails\":{\"curationNote\":\"mine to edit\"}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertOk( r );
        }
    }

    /**
     * A preflight writes nothing, so it is deliberately exempt. Refusing it would stop a curator
     * finding out what a commit WOULD do while somebody else holds the lock — which is exactly the
     * moment they most want to know.
     */
    @Test
    public void testPreflightIsNotGatedByAnotherCuratorsLock() {
        testAuthenticationUtils.runAsAgent();
        takeLockFor( "batchalice" );
        testAuthenticationUtils.runAsAdmin();

        String body = "{\"curationDetails\":{\"curationNote\":\"what would this do\"}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/preflight" ).request()
                .post( Entity.json( body ) ) ) {
            assertOk( r );
        }
    }

    // ============================================================================================
    // Bulk locks — POST /datasets/curation/locks
    // ============================================================================================

    /**
     * The read uib asked for: the curation queue pages up to 1000 rows and needs one request per screen,
     * not one per row.
     */
    @Test
    public void testBulkLockReadReportsWhoHoldsEachDataset() {
        testAuthenticationUtils.runAsAgent();
        takeLockFor( "queuealice" );
        testAuthenticationUtils.runAsAdmin();

        try ( Response r = target( "/datasets/curation/locks" )
                .queryParam( "datasets", ee.getId() ).request().get() ) {
            assertOk( r );
            String json = r.readEntity( String.class );
            assertThat( json ).contains( String.valueOf( ee.getId() ) ).contains( "queuealice" );
        }
    }

    /**
     * uib's ask: a blocked curator gets a name and has to choose wait-or-steal, and a batch mid-run looks
     * exactly like a person at lunch. `?runId=` names the job, and it comes back on the read.
     * <p>
     * 🛑 Recorded on the LOCK rather than joined from the holder's draft, because a batch takes its locks
     * BEFORE doing the work — at the moment a curator is blocked there may be no draft to join to.
     */
    @Test
    public void testTheLockSaysWhatIsHoldingItNotJustWho() {
        testAuthenticationUtils.runAsAgent();
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" )
                .queryParam( "onBehalfOf", "batchalice" )
                .queryParam( "runId", "category-policy-rebuild-2026-08-09" )
                .queryParam( "agentName", "proposer" )
                .request().post( Entity.json( "{}" ) ) ) {
            assertOk( r );
        }
        testAuthenticationUtils.runAsAdmin();

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" ).request().get() ) {
            assertOk( r );
            String json = r.readEntity( String.class );
            assertThat( json ).as( "who it is for" ).contains( "batchalice" );
            assertThat( json ).as( "and WHAT is holding it" )
                    .contains( "category-policy-rebuild-2026-08-09" ).contains( "proposer" );
        }
    }

    /** A person takes a lock without naming a job, and it reads as a person: no run id. */
    @Test
    public void testAHumanHeldLockNamesNoJob() {
        takeLockFor( null );
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" ).request().get() ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) )
                    .as( "no runId means a person, which is the distinction the field exists to make" )
                    .doesNotContain( "category-policy-rebuild" );
        }
    }

    /**
     * The same answer with the ids in a body. Exists because a thousand ids is ~7 KB of query string against
     * an 8 KB container header limit, so the queue's largest page sits on the boundary — and past it the
     * container refuses the request with a 400 that never mentions datasets.
     */
    @Test
    public void testBulkLockReadAcceptsAlargeListInAbody() {
        testAuthenticationUtils.runAsAgent();
        takeLockFor( "queuebob" );
        testAuthenticationUtils.runAsAdmin();

        String body = "{\"datasetIds\":[" + ee.getId() + "]}";
        try ( Response r = target( "/datasets/curation/locks/query" ).request().post( Entity.json( body ) ) ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) )
                    .contains( String.valueOf( ee.getId() ) ).contains( "queuebob" );
        }
    }

    /**
     * 🛑 An unheld dataset is ABSENT from the map rather than present with {@code locked:false}. A queue
     * painting 1000 rows should not be sent 1000 entries to say nothing is happening.
     */
    @Test
    public void testBulkLockReadOmitsDatasetsNobodyHolds() {
        try ( Response r = target( "/datasets/curation/locks" )
                .queryParam( "datasets", ee.getId() ).request().get() ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) )
                    .as( "nobody holds it, so it is not in the map" )
                    .doesNotContain( "lockedBy" );
        }
    }

    /**
     * The batch never fails as a unit. One dataset held by someone else must not sink the claim over
     * the rest, so each id carries its own outcome and the incumbent is named.
     */
    @Test
    public void testBulkLockReportsPerDatasetRatherThanFailingTheBatch() {
        testAuthenticationUtils.runAsAgent();
        takeLockFor( "batchalice" );
        testAuthenticationUtils.runAsAdmin();

        String body = "{\"datasetIds\":[" + ee.getId() + "]}";
        try ( Response r = target( "/datasets/curation/locks" ).request().post( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).as( "the batch itself succeeds" )
                    .isEqualTo( Response.Status.OK.getStatusCode() );
            String json = r.readEntity( String.class );
            assertThat( json ).contains( "\"granted\":false" ).contains( "heldByAnother" );
            assertThat( json ).as( "and names the incumbent so the caller can report it" ).contains( "batchalice" );
        }
    }

    /** A free dataset is granted, and the claim is then visible to the single-dataset read. */
    @Test
    public void testBulkLockGrantsAFreeDatasetAndTheClaimIsVisible() {
        String body = "{\"datasetIds\":[" + ee.getId() + "]}";
        try ( Response r = target( "/datasets/curation/locks" ).request().post( Entity.json( body ) ) ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) ).contains( "\"granted\":true" );
        }
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" ).request().get() ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) ).contains( "\"locked\":true" );
        }
    }

    /**
     * The companion a finished run has to call. Without it a completed batch's claims sit until the
     * lease lapses, gating curators on datasets nothing is working on any more.
     */
    @Test
    public void testBulkReleaseDropsTheCallersOwnClaims() {
        String body = "{\"datasetIds\":[" + ee.getId() + "]}";
        try ( Response r = target( "/datasets/curation/locks" ).request().post( Entity.json( body ) ) ) {
            assertOk( r );
        }
        try ( Response r = target( "/datasets/curation/locks/release" ).request().post( Entity.json( body ) ) ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) ).contains( "true" );
        }
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation/lock" ).request().get() ) {
            assertOk( r );
            assertThat( r.readEntity( String.class ) ).contains( "\"locked\":false" );
        }
    }
}
