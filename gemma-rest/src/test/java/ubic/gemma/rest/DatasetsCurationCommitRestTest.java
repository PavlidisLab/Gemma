package ubic.gemma.rest;

import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExperimentalDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValueBasicValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest5;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
        // read filter (which deliberately drops free-text tags).
        String add = "{\"tags\":{\"items\":[{\"clientRef\":\"T1\","
                + "\"category\":{\"label\":\"disease\",\"uri\":\"http://purl.obolibrary.org/obo/DOID_4\"},"
                + "\"value\":{\"label\":\"glioma\",\"uri\":\"http://purl.obolibrary.org/obo/DOID_0060108\"}}]}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( add ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
            assertThat( r.readEntity( String.class ) ).contains( "T1" );
        }
        AnnotationValueObject glioma = findAnnotation( "glioma" );
        assertThat( glioma ).as( "tag was persisted" ).isNotNull();

        // Delete it by its (now-known) id via deletedIds.
        String del = "{\"tags\":{\"items\":[],\"deletedIds\":[" + glioma.getId() + "]}}";
        try ( Response r = target( "/datasets/" + ee.getId() + "/curation" ).request().put( Entity.json( del ) ) ) {
            assertThat( r.getStatus() ).isEqualTo( Response.Status.OK.getStatusCode() );
        }
        assertThat( findAnnotation( "glioma" ) ).as( "tag was removed by deletedIds" ).isNull();
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
            assertThat( r.readEntity( String.class ) ).contains( "S1" );
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

    private AnnotationValueObject findAnnotation( String termName ) {
        Set<AnnotationValueObject> annotations = expressionExperimentService.getAnnotations( expressionExperimentService.load( ee.getId() ) );
        return annotations.stream().filter( a -> termName.equals( a.getTermName() ) ).findFirst().orElse( null );
    }
}
