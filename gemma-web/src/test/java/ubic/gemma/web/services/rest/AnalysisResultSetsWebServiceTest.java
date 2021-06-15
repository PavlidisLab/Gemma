package ubic.gemma.web.services.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisDao;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetDao;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.args.*;
import ubic.gemma.web.util.BaseSpringWebTest;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response;
import java.util.Collection;
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

    /* fixtures */
    private ExpressionExperiment ee;
    private DifferentialExpressionAnalysis dea;
    private ExpressionAnalysisResultSet dears;
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
    }

    @Test
    public void testFindAllWhenNoDatasetsAreProvidedThenReturnLatestAnalysisResults() {
        HttpServletResponse response = new MockHttpServletResponse();
        ResponseDataObject<?> result = service.findAll( null,
                null,
                IntArg.valueOf( "0" ),
                IntArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                response );
        assertEquals( response.getStatus(), 200 );
        List<ExpressionAnalysisResultSetValueObject> results = ( List<ExpressionAnalysisResultSetValueObject> ) result.getData();
        assertEquals( results.size(), 1 );
    }

    @Test
    public void testFindAllWithDatasetIdsThenReturnLatestAnalysisResults() {
        HttpServletResponse response = new MockHttpServletResponse();
        ArrayDatasetArg datasets = ArrayDatasetArg.valueOf( String.valueOf( ee.getId() ) );
        ResponseDataObject<?> result = service.findAll(
                datasets,
                null,
                IntArg.valueOf( "0" ),
                IntArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                response );
        assertEquals( response.getStatus(), 200 );
        List<ExpressionAnalysisResultSetValueObject> results = ( List<ExpressionAnalysisResultSetValueObject> ) result.getData();
        assertEquals( results.get( 0 ).getId(), dears.getId() );
    }

    @Test
    public void testFindAllWhenDatasetDoesNotExistThenRaise404NotFound() {
        HttpServletResponse response = new MockHttpServletResponse();
        GemmaApiException e = assertThrows( GemmaApiException.class, () -> service.findAll(
                ArrayDatasetArg.valueOf( "GEO123124" ),
                null,
                IntArg.valueOf( "0" ),
                IntArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                response ) );
        assertEquals( e.getCode(), Response.Status.NOT_FOUND );
    }

    @Test
    public void testFindAllWithDatabaseEntriesThenReturnLatestAnalysisResults() {
        HttpServletResponse response = new MockHttpServletResponse();
        ResponseDataObject<?> result = service.findAll( null,
                ArrayDatabaseEntryArg.valueOf( ee.getAccession().getAccession() ),
                IntArg.valueOf( "0" ),
                IntArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                response );
        assertEquals( response.getStatus(), 200 );
        List<ExpressionAnalysisResultSetValueObject> results = ( List<ExpressionAnalysisResultSetValueObject> ) result.getData();
        assertEquals( results.get( 0 ).getId(), dears.getId() );
    }

    @Test
    public void testFindAllWhenDatabaseEntryDoesNotExistThenRaise404NotFound() {
        HttpServletResponse response = new MockHttpServletResponse();
        GemmaApiException e = assertThrows( GemmaApiException.class, () -> service.findAll(
                null,
                ArrayDatabaseEntryArg.valueOf( "GEO123124,GEO1213121" ),
                IntArg.valueOf( "0" ),
                IntArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                response ) );
        assertEquals( e.getCode(), Response.Status.NOT_FOUND );
    }

    @Test
    public void testFindByIdThenReturn200Success() {
        HttpServletResponse response = new MockHttpServletResponse();
        ResponseDataObject<?> result = service.findById( new ExpressionAnalysisResultSetArg( dears.getId() ), response );
        assertEquals( response.getStatus(), 200 );
        ExpressionAnalysisResultSetValueObject dearsVo = ( ExpressionAnalysisResultSetValueObject ) result.getData();
        assertEquals( dearsVo.getId(), dears.getId() );
        assertEquals( dearsVo.getAnalysis().getId(), dea.getId() );
    }

    @Test
    public void testFindByIdWhenResultSetDoesNotExistsThenReturn404NotFoundError() {
        Long id = 123129L;
        HttpServletResponse response = new MockHttpServletResponse();
        GemmaApiException e = assertThrows( GemmaApiException.class, () -> {
            service.findById( new ExpressionAnalysisResultSetArg( id ), response );
        } );
        assertEquals( e.getCode(), Response.Status.NOT_FOUND );
    }
}