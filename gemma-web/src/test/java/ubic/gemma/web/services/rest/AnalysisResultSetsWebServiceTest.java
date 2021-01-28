package ubic.gemma.web.services.rest;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.AnalysisResultSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.web.services.rest.util.GemmaApiException;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.args.DatabaseEntryArg;
import ubic.gemma.web.services.rest.util.args.ExpressionAnalysisResultSetArg;
import ubic.gemma.web.services.rest.util.args.DatasetArg;
import ubic.gemma.web.services.rest.util.args.DatasetIdArg;
import ubic.gemma.web.util.BaseSpringWebTest;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import java.util.*;

import static org.junit.Assert.*;

public class AnalysisResultSetsWebServiceTest extends BaseSpringWebTest {

    @Autowired
    private AnalysisResultSetsWebService service;

    @Test
    public void testFindAllWhenNoDatasetsAreProvidedThenReturnLatestAnalysisResults() {
        HttpServletResponse response = new MockHttpServletResponse();
        service.findAll( null,
                null,
                response );
        assertEquals( response.getStatus(), 200 );
    }

    @Test
    public void testFindAllWithDatasetIdsThenReturnLatestAnalysisResults() {
        HttpServletResponse response = new MockHttpServletResponse();
        ExpressionExperiment ee = getTestPersistentExpressionExperiment();
        Collection<Analysis> analyses = addTestAnalyses( ee );
        assertFalse( ee.getBioAssays().isEmpty() );
        Set<DatasetArg<?>> datasetIds = Collections.singleton( new DatasetIdArg( ee.getId() ) );
        ResponseDataObject<List<AnalysisResultSetValueObject>> result = service.findAll(
                datasetIds,
                null,
                response );
        assertFalse( result.getData().isEmpty() );
        assertEquals( response.getStatus(), 200 );
    }

    @Test
    public void testFindAllWithExternalIdsThenReturnLatestAnalysisResults() {
        HttpServletResponse response = new MockHttpServletResponse();
        service.findAll( null,
                new HashSet<>( Arrays.asList( DatabaseEntryArg.valueOf( "GEO123123" ), DatabaseEntryArg.valueOf( "GEO1213121" ) ) ),
                response );
        assertEquals( response.getStatus(), 200 );
    }

    @Test
    public void testFindByIdThenReturn200Success() {
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment();
        Collection<Analysis> analyses = addTestAnalyses( ee );
        Analysis firstAnalysis = analyses.stream().findFirst().get();
        HttpServletResponse response = new MockHttpServletResponse();
        service.findById( new ExpressionAnalysisResultSetArg( firstAnalysis.getId() ), response );
        assertEquals( response.getStatus(), 200 );
    }

    @Test
    public void testFindByIdWhenResultSetDoesNotExistsThenReturn404NotFoundError() {
        Long id = 1L;
        HttpServletResponse response = new MockHttpServletResponse();
        GemmaApiException e = assertThrows( GemmaApiException.class, () -> {
            service.findById( new ExpressionAnalysisResultSetArg( id ), response );
        } );
        assertEquals( e.getCode(), 404 );
    }
}