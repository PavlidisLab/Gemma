package ubic.gemma.rest;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.analysis.expression.diff.*;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStreamReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static ubic.gemma.rest.util.Assertions.assertThat;

public class AnalysisResultSetsWebServiceTest extends BaseJerseyIntegrationTest {

    @Autowired
    private AnalysisResultSetsWebService service;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private DatabaseEntryService databaseEntryService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    /* fixtures */
    private ArrayDesign arrayDesign;
    private ExpressionExperiment ee;
    private DifferentialExpressionAnalysis dea;
    private ExpressionAnalysisResultSet dears;
    private DatabaseEntry databaseEntry2;

    @Autowired
    private PersistentDummyObjectHelper testHelper;

    @Before
    public void setupMocks() {

        ee = testHelper.getTestPersistentBasicExpressionExperiment();

        arrayDesign = testHelper.getTestPersistentArrayDesign( 1, true, true );
        CompositeSequence probe = arrayDesign.getCompositeSequences().stream().findFirst().orElse( null );
        assertNotNull( probe );
        assertNotNull( probe.getId() );

        dea = new DifferentialExpressionAnalysis();
        dea.setExperimentAnalyzed( ee );
        dears = new ExpressionAnalysisResultSet();
        dears.setAnalysis( dea );
        PvalueDistribution pvalueDist = new PvalueDistribution();
        pvalueDist.setBinCounts( new double[0] );
        pvalueDist.setNumBins( 0 );
        dears.setPvalueDistribution( pvalueDist );
        dea.getResultSets().add( dears );

        DifferentialExpressionAnalysisResult dear = DifferentialExpressionAnalysisResult.Factory.newInstance();
        dear.setPvalue( 1.0 );
        dear.setCorrectedPvalue( 0.0001 );
        dear.setResultSet( dears );
        dear.setProbe( probe );
        dears.getResults().add( dear );

        dea = differentialExpressionAnalysisService.create( dea );

        ExternalDatabase geo = externalDatabaseService.findByName( ExternalDatabases.GEO );
        assertNotNull( geo );
        assertEquals( ExternalDatabases.GEO, geo.getName() );

        DatabaseEntry databaseEntry = DatabaseEntry.Factory.newInstance();
        databaseEntry.setAccession( "GEO123123" );
        databaseEntry.setExternalDatabase( geo );
        //noinspection ResultOfMethodCallIgnored
        databaseEntryService.create( databaseEntry );

        databaseEntry2 = DatabaseEntry.Factory.newInstance();
        databaseEntry2.setAccession( "GEO1213121" );
        databaseEntry2.setExternalDatabase( geo );
        databaseEntry2 = databaseEntryService.create( databaseEntry2 );
    }

    @After
    public void removeFixtures() {
        differentialExpressionAnalysisService.remove( dea );
        expressionExperimentService.remove( ee );
        databaseEntryService.remove( databaseEntry2 );
        arrayDesignService.remove( arrayDesign );
    }

    @Test
    public void testFindAllWhenNoDatasetsAreProvidedThenReturnLatestAnalysisResults() {
        ResponseDataObject<?> result = service.getResultSets( null,
                null,
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ) );
        //noinspection unchecked
        List<DifferentialExpressionAnalysisResultSetValueObject> results = ( ( List<DifferentialExpressionAnalysisResultSetValueObject> ) result.getData() );

        // this is kind of annoying, but we can have results from other tests still lingering in the database, so we
        // only need to check for the fixture
        assertThat( results )
                .extracting( "id" )
                .contains( this.dears.getId() );

        // individual analysis results are not exposed from this endpoint
        assertThat( results ).extracting( "results" )
                .containsOnlyNulls();
    }

    @Test
    public void testFindAllWithFilters() {
        ResponseDataObject<?> result = service.getResultSets( null,
                null,
                FilterArg.valueOf( "id = " + this.dears.getId() ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ) );
        //noinspection unchecked
        List<DifferentialExpressionAnalysisResultSetValueObject> results = ( List<DifferentialExpressionAnalysisResultSetValueObject> ) result.getData();
        assertEquals( results.size(), 1 );
        // individual analysis results are not exposed from this endpoint
        assertNull( results.get( 0 ).getResults() );
    }

    @Test
    public void testFindAllWithFiltersAndCollections() {
        ResponseDataObject<?> result = service.getResultSets( null,
                null,
                FilterArg.valueOf( "id in (" + this.dears.getId() + ")" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ) );
        //noinspection unchecked
        List<DifferentialExpressionAnalysisResultSetValueObject> results = ( List<DifferentialExpressionAnalysisResultSetValueObject> ) result.getData();
        assertEquals( results.size(), 1 );
        // individual analysis results are not exposed from this endpoint
        assertNull( results.get( 0 ).getResults() );
    }

    @Test
    public void testFindAllWithInvalidFilters() {
        assertThrows( BadRequestException.class, () -> service.getResultSets( null,
                null,
                FilterArg.valueOf( "id2 = " + this.dears.getId() ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ) ) );
    }

    @Test
    public void testFindAllWithDatasetIdsThenReturnLatestAnalysisResults() {
        DatasetArrayArg datasets = DatasetArrayArg.valueOf( String.valueOf( ee.getId() ) );
        ResponseDataObject<?> result = service.getResultSets(
                datasets,
                null,
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ) );
        //noinspection unchecked
        List<DifferentialExpressionAnalysisResultSetValueObject> results = ( List<DifferentialExpressionAnalysisResultSetValueObject> ) result.getData();
        assertEquals( results.get( 0 ).getId(), dears.getId() );
    }

    @Test
    public void testFindAllWhenDatasetDoesNotExistThenRaise404NotFound() {
        NotFoundException e = assertThrows( NotFoundException.class, () -> service.getResultSets(
                DatasetArrayArg.valueOf( "GEO123124" ),
                null,
                null,
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ) ) );
        assertEquals( e.getResponse().getStatus(), Response.Status.NOT_FOUND.getStatusCode() );
    }

    @Test
    public void testFindAllWithDatabaseEntriesThenReturnLatestAnalysisResults() {
        assertThat( ee.getAccession() ).isNotNull();
        ResponseDataObject<?> result = service.getResultSets( null,
                DatabaseEntryArrayArg.valueOf( ee.getAccession().getAccession() ),
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ) );
        //noinspection unchecked
        List<DifferentialExpressionAnalysisResultSetValueObject> results = ( List<DifferentialExpressionAnalysisResultSetValueObject> ) result.getData();
        assertEquals( results.get( 0 ).getId(), dears.getId() );
    }

    @Test
    public void testFindAllWhenDatabaseEntryDoesNotExistThenRaise404NotFound() {
        NotFoundException e = assertThrows( NotFoundException.class, () -> service.getResultSets(
                null,
                DatabaseEntryArrayArg.valueOf( "GEO123124,GEO1213121" ),
                null,
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ) ) );
        assertEquals( e.getResponse().getStatus(), Response.Status.NOT_FOUND.getStatusCode() );
    }

    @Test
    public void testFindByIdThenReturn200Success() {
        assertThat( target( "/resultSets/" + dears.getId() ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaType( MediaType.APPLICATION_JSON_TYPE )
                .hasEncoding( "gzip" )
                .entity()
                .hasFieldOrPropertyWithValue( "data.id", dears.getId().intValue() )
                .hasFieldOrPropertyWithValue( "data.analysis.id", dea.getId().intValue() )
                .extracting( "data.results" )
                .isNotNull();
    }

    @Test
    public void testFindByIdWhenExcludeResultsThenReturn200Success() {
        assertThat( target( "/resultSets/" + dears.getId() ).queryParam( "excludeResults", true ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaType( MediaType.APPLICATION_JSON_TYPE )
                .hasEncoding( "gzip" )
                .entityAsString()
                .doesNotContain( "results" );
    }

    @Test
    public void testFindByIdWhenInvalidIdentifierThenThrowMalformedArgException() {
        assertThat( target( "/resultSets/alksdok102" ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST );
    }

    @Test
    public void testFindByIdWhenResultSetDoesNotExistsThenReturn404NotFoundError() {
        assertThat( target( "/resultSets/" + 123129L ).request().get() )
                .hasStatus( Response.Status.NOT_FOUND );
    }

    @Test
    public void testFindByIdToTsv() {
        assertThat( target( "/resultSets/" + dears.getId() ).request( "text/tab-separated-values" ).get() )
                .hasMediaType( new MediaType( "text", "tab-separated-values", "UTF-8" ) )
                .entityAsStream()
                .satisfies( is -> {
                    CSVParser reader = CSVFormat.TDF.builder()
                            .setHeader()
                            .setSkipHeaderRecord( true )
                            .setCommentMarker( '#' )
                            .get()
                            .parse( new InputStreamReader( is ) );
                    assertThat( reader.getHeaderNames() ).containsExactly( "id", "probe_id", "probe_name",
                            "gene_id", "gene_name", "gene_ncbi_id", "gene_ensembl_id", "gene_official_symbol",
                            "gene_official_name", "pvalue", "corrected_pvalue", "rank" );
                    CSVRecord record = reader.iterator().next();
                    assertEquals( "1.0", record.get( "pvalue" ) );
                    assertEquals( "0.0001", record.get( "corrected_pvalue" ) );
                    // rank is null, it should appear as an empty string
                    assertEquals( "", record.get( "rank" ) );
                } );
    }
}