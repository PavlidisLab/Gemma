package ubic.gemma.rest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.core.security.SecurityService;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.core.util.test.TestAuthenticationUtils;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;
import ubic.gemma.model.expression.experiment.StatementValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSubSetService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest5;

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
        ExperimentalDesignValueObject.ExperimentalFactorEntry factor = before.getExperimentalFactors().stream()
                .filter( f -> f.getValues().stream().anyMatch( v -> countAssigned( before, v.getId() ) > 0 ) )
                .findFirst()
                .orElseThrow( () -> new AssertionError( "seeded design has no assigned factor value" ) );
        FactorValueBasicValueObject anchor = factor.getValues().stream()
                .filter( v -> countAssigned( before, v.getId() ) > 0 )
                .findFirst()
                .orElseThrow();
        Set<Long> anchorBmIds = before.getBioMaterialAssignments().stream()
                .filter( a -> a.getFactorValueIds().contains( anchor.getId() ) )
                .map( ExperimentalDesignValueObject.BioMaterialFactorValueAssignment::getBioMaterialId )
                .collect( Collectors.toSet() );

        // A subset defined by exactly the samples carrying that factor value — deleting it strands the subset.
        // Built inside a transaction: the create cascades over each BioAssay's sample and its lazy factor-value
        // collections, which are detached once thawBioAssays' session closes.
        ExpressionExperimentSubSet subset = new TransactionTemplate( transactionManager ).execute( status -> {
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
        assertThat( subset ).isNotNull();

        String body = "{\"design\":{\"factors\":{\"items\":[{\"gemmaId\":" + factor.getId() + ","
                + "\"factorValues\":{\"items\":[],\"deletedIds\":[" + anchor.getId() + "]}}]}}}";

        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( body ) ) ) {
            assertThat( r.getStatus() ).as( "stranding a subset is a consent question, not a silent success" )
                    .isEqualTo( Response.Status.CONFLICT.getStatusCode() );
            assertThat( r.readEntity( String.class ) ).contains( "subset" );
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

    private AnnotationValueObject findAnnotation( String termName ) {
        Set<AnnotationValueObject> annotations = expressionExperimentService.getAnnotations( expressionExperimentService.load( ee.getId() ) );
        return annotations.stream().filter( a -> termName.equals( a.getTermName() ) ).findFirst().orElse( null );
    }
}
