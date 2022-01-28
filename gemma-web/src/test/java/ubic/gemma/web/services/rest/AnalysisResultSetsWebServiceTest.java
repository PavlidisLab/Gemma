package ubic.gemma.web.services.rest;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.services.rest.util.MalformedArgException;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.args.*;
import ubic.gemma.web.util.BaseSpringWebTest;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class AnalysisResultSetsWebServiceTest extends BaseSpringWebTest {

    @Autowired
    private AnalysisResultSetsWebService service;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;

    @Autowired
    private DatabaseEntryService databaseEntryService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    /* fixtures */
    private ArrayDesign arrayDesign;
    private CompositeSequence probe;
    private ExpressionExperiment ee;
    private DifferentialExpressionAnalysis dea;
    private ExpressionAnalysisResultSet dears;
    private DifferentialExpressionAnalysisResult dear;
    private DatabaseEntry databaseEntry;
    private DatabaseEntry databaseEntry2;

    @Before
    public void setUp() {
        ee = getTestPersistentBasicExpressionExperiment();

        dea = new DifferentialExpressionAnalysis();
        dea.setExperimentAnalyzed( ee );
        dea = differentialExpressionAnalysisService.create( dea );

        dears = new ExpressionAnalysisResultSet();
        dears.setAnalysis( dea );
        dears = expressionAnalysisResultSetService.create( dears );

        arrayDesign = testHelper.getTestPersistentArrayDesign( 1, true, true );
        probe = arrayDesign.getCompositeSequences().stream().findFirst().orElse( null );
        assertNotNull( probe );

        dear = DifferentialExpressionAnalysisResult.Factory.newInstance();
        dear.setPvalue( 1.0 );
        dear.setCorrectedPvalue( 0.0001 );
        dear.setResultSet( dears );
        dear.setProbe( probe );
        dears.setResults( Collections.singleton( dear ) );
        expressionAnalysisResultSetService.update( dears );

        ExternalDatabase geo = externalDatabaseService.findByName( "GEO" );
        assertNotNull( geo );
        assertEquals( geo.getName(), "GEO" );

        databaseEntry = DatabaseEntry.Factory.newInstance();
        databaseEntry.setAccession( "GEO123123" );
        databaseEntry.setExternalDatabase( geo );
        databaseEntry = databaseEntryService.create( databaseEntry );

        databaseEntry2 = DatabaseEntry.Factory.newInstance();
        databaseEntry2.setAccession( "GEO1213121" );
        databaseEntry2.setExternalDatabase( geo );
        databaseEntry2 = databaseEntryService.create( databaseEntry2 );
    }

    @After
    public void tearDown() {
        expressionAnalysisResultSetService.remove( dears );
        differentialExpressionAnalysisService.remove( dea );
        expressionExperimentService.remove( ee );
        databaseEntryService.remove( databaseEntry2 );
        arrayDesignService.remove( arrayDesign );
    }

    @Test
    public void testFindAllWhenNoDatasetsAreProvidedThenReturnLatestAnalysisResults() {
        HttpServletResponse response = new MockHttpServletResponse();
        ResponseDataObject<?> result = service.findAll( null,
                null,
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                response );
        assertEquals( response.getStatus(), 200 );
        //noinspection unchecked
        List<ExpressionAnalysisResultSetValueObject> results = ( List<ExpressionAnalysisResultSetValueObject> ) result.getData();
        assertEquals( results.size(), 1 );
        // individual analysis results are not exposed from this endpoint
        assertNull( results.get( 0 ).getResults() );
    }

    @Test
    public void testFindAllWithFilters() {
        HttpServletResponse response = new MockHttpServletResponse();
        ResponseDataObject<?> result = service.findAll( null,
                null,
                FilterArg.valueOf( "id = " + this.dears.getId() ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                response );
        assertEquals( response.getStatus(), 200 );
        //noinspection unchecked
        List<ExpressionAnalysisResultSetValueObject> results = ( List<ExpressionAnalysisResultSetValueObject> ) result.getData();
        assertEquals( results.size(), 1 );
        // individual analysis results are not exposed from this endpoint
        assertNull( results.get( 0 ).getResults() );
    }

    @Test
    public void testFindAllWithFiltersAndCollections() {
        HttpServletResponse response = new MockHttpServletResponse();
        ResponseDataObject<?> result = service.findAll( null,
                null,
                FilterArg.valueOf( "id in (" + this.dears.getId() + ")" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                response );
        assertEquals( response.getStatus(), 200 );
        //noinspection unchecked
        List<ExpressionAnalysisResultSetValueObject> results = ( List<ExpressionAnalysisResultSetValueObject> ) result.getData();
        assertEquals( results.size(), 1 );
        // individual analysis results are not exposed from this endpoint
        assertNull( results.get( 0 ).getResults() );
    }

    @Test
    public void testFindAllWithInvalidFilters() {
        HttpServletResponse response = new MockHttpServletResponse();
        assertThrows( BadRequestException.class, () -> service.findAll( null,
                null,
                FilterArg.valueOf( "id2 = " + this.dears.getId() ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                response ) );
    }

    @Test
    public void testFindAllWithDatasetIdsThenReturnLatestAnalysisResults() {
        HttpServletResponse response = new MockHttpServletResponse();
        DatasetArrayArg datasets = DatasetArrayArg.valueOf( String.valueOf( ee.getId() ) );
        ResponseDataObject<?> result = service.findAll(
                datasets,
                null,
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                response );
        assertEquals( response.getStatus(), 200 );
        //noinspection unchecked
        List<ExpressionAnalysisResultSetValueObject> results = ( List<ExpressionAnalysisResultSetValueObject> ) result.getData();
        assertEquals( results.get( 0 ).getId(), dears.getId() );
    }

    @Test
    public void testFindAllWhenDatasetDoesNotExistThenRaise404NotFound() {
        HttpServletResponse response = new MockHttpServletResponse();
        NotFoundException e = assertThrows( NotFoundException.class, () -> service.findAll(
                DatasetArrayArg.valueOf( "GEO123124" ),
                null,
                null,
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                response ) );
        assertEquals( e.getResponse().getStatus(), Response.Status.NOT_FOUND.getStatusCode() );
    }

    @Test
    public void testFindAllWithDatabaseEntriesThenReturnLatestAnalysisResults() {
        HttpServletResponse response = new MockHttpServletResponse();
        ResponseDataObject<?> result = service.findAll( null,
                DatabaseEntryArrayArg.valueOf( ee.getAccession().getAccession() ),
                FilterArg.valueOf( "" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                response );
        assertEquals( response.getStatus(), 200 );
        //noinspection unchecked
        List<ExpressionAnalysisResultSetValueObject> results = ( List<ExpressionAnalysisResultSetValueObject> ) result.getData();
        assertEquals( results.get( 0 ).getId(), dears.getId() );
    }

    @Test
    public void testFindAllWhenDatabaseEntryDoesNotExistThenRaise404NotFound() {
        HttpServletResponse response = new MockHttpServletResponse();
        NotFoundException e = assertThrows( NotFoundException.class, () -> service.findAll(
                null,
                DatabaseEntryArrayArg.valueOf( "GEO123124,GEO1213121" ),
                null,
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                response ) );
        assertEquals( e.getResponse().getStatus(), Response.Status.NOT_FOUND.getStatusCode() );
    }

    @Test
    public void testFindByIdThenReturn200Success() {
        HttpServletResponse response = new MockHttpServletResponse();
        ResponseDataObject<?> result = service.findById( ExpressionAnalysisResultSetArg.valueOf( dears.getId().toString() ), false );
        assertEquals( response.getStatus(), 200 );
        ExpressionAnalysisResultSetValueObject dearsVo = ( ExpressionAnalysisResultSetValueObject ) result.getData();
        assertEquals( dearsVo.getId(), dears.getId() );
        assertEquals( dearsVo.getAnalysis().getId(), dea.getId() );
        assertNotNull( dearsVo.getResults() );
    }

    @Test
    public void testFindByIdWhenExcludeResultsThenReturn200Success() {
        HttpServletResponse response = new MockHttpServletResponse();
        ResponseDataObject<?> result = service.findById( ExpressionAnalysisResultSetArg.valueOf( dears.getId().toString() ), true );
        assertEquals( response.getStatus(), 200 );
        ExpressionAnalysisResultSetValueObject dearsVo = ( ExpressionAnalysisResultSetValueObject ) result.getData();
        assertEquals( dearsVo.getId(), dears.getId() );
        assertEquals( dearsVo.getAnalysis().getId(), dea.getId() );
        assertNull( dearsVo.getResults() );
    }

    @Test
    public void testFindByIdWhenInvalidIdentifierThenThrowMalformedArgException() {
        HttpServletResponse response = new MockHttpServletResponse();
        assertThrows( MalformedArgException.class, () -> service.findById( ExpressionAnalysisResultSetArg.valueOf( "alksdok102" ), false ) );
    }

    @Test
    public void testFindByIdWhenResultSetDoesNotExistsThenReturn404NotFoundError() {
        long id = 123129L;
        HttpServletResponse response = new MockHttpServletResponse();
        NotFoundException e = assertThrows( NotFoundException.class, () -> service.findById( ExpressionAnalysisResultSetArg.valueOf( String.valueOf( id ) ), false ) );
        assertEquals( e.getResponse().getStatus(), Response.Status.NOT_FOUND.getStatusCode() );
    }

    @Test
    public void testFindByIdToTsv() throws IOException {
        HttpServletResponse response = new MockHttpServletResponse();
        StreamingOutput result = service.findByIdToTsv( ExpressionAnalysisResultSetArg.valueOf( dears.getId().toString() ), response );
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        result.write( byteArrayOutputStream );
        byteArrayOutputStream.toString( StandardCharsets.UTF_8.name() );
        // FIXME: I could not find the equivalent of withFirstRecordAsHeader() in the builder API
        CSVParser reader = CSVFormat.Builder.create( CSVFormat.TDF.withFirstRecordAsHeader() )
                .setSkipHeaderRecord( false )
                .setCommentMarker( '#' ).build()
                .parse( new InputStreamReader( new ByteArrayInputStream( byteArrayOutputStream.toByteArray() ) ) );
        assertEquals( reader.getHeaderNames(), Arrays.asList( "id", "probe_id", "probe_name", "gene_id", "gene_name", "gene_ncbi_id", "gene_official_symbol", "gene_official_name", "pvalue", "corrected_pvalue", "rank" ) );
        CSVRecord record = reader.iterator().next();
        assertEquals( record.get( "pvalue" ), "1E0" );
        assertEquals( record.get( "corrected_pvalue" ), "1E-4" );
        // rank is null, it should appear as an empty string
        assertEquals( record.get( "rank" ), "" );
    }
}