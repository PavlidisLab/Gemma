package ubic.gemma.web.services.rest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import ubic.gemma.core.testing.BaseSpringWebTest;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.args.*;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * @author tesarst
 */
public class DatasetsRestTest extends BaseSpringWebTest {

    private static boolean loaded = false;
    private static ArrayList<ExpressionExperiment> ees = new ArrayList<>( 20 );
    @Autowired
    private DatasetsWebService service;

    @Before
    public void setUp() throws Exception {
        if ( loaded ) {
            return;
        }
        for ( int i = 0; i < 10; i++ ) {
            ees.add( getNewTestPersistentCompleteExpressionExperiment() );
        }
        loaded = true;
    }

    @Test
    public void testAll() throws Exception {
        ResponseDataObject response = service
                .all( DatasetFilterArg.valueOf( "" ), IntArg.valueOf( "5" ), IntArg.valueOf( "5" ),
                        SortArg.valueOf( "+id" ), new MockHttpServletResponse() );

        assertNotNull( response.getData() );
        assertTrue( response.getData() instanceof Collection<?> );
        assertEquals( 5, ( ( Collection ) response.getData() ).size() );
        assertTrue(
                ( ( Collection ) response.getData() ).iterator().next() instanceof ExpressionExperimentValueObject );
    }

    @Test
    public void testSome() throws Exception {
        ResponseDataObject response = service.datasets(
                ArrayDatasetArg.valueOf( ees.get( 0 ).getShortName() + ", BAD_NAME, " + ees.get( 2 ).getShortName() ),
                DatasetFilterArg.valueOf( "" ), IntArg.valueOf( "0" ), IntArg.valueOf( "10" ), SortArg.valueOf( "+id" ),
                new MockHttpServletResponse() );

        assertNotNull( response.getData() );
        assertTrue( response.getData() instanceof Collection<?> );
        assertEquals( 2, ( ( Collection ) response.getData() ).size() );
        assertTrue(
                ( ( Collection ) response.getData() ).iterator().next() instanceof ExpressionExperimentValueObject );
    }

    @Test
    public void testPlatforms() throws Exception {
        //noinspection unchecked
        ResponseDataObject response = service
                .datasetPlatforms( DatasetArg.valueOf( String.valueOf( ees.get( 1 ).getId() ) ),
                        new MockHttpServletResponse() );

        assertNotNull( response.getData() );
        assertTrue( response.getData() instanceof Collection<?> );
        assertTrue( ( ( Collection ) response.getData() ).iterator().next() instanceof ArrayDesignValueObject );
    }

    @Test
    public void testSamples() throws Exception {
        //noinspection unchecked
        ResponseDataObject response = service
                .datasetPlatforms( DatasetArg.valueOf( String.valueOf( ees.get( 1 ).getId() ) ),
                        new MockHttpServletResponse() );

        assertNotNull( response.getData() );
        assertTrue( response.getData() instanceof Collection<?> );
        // since we are using mock ees, the bioassays array will likely be empty.
    }

    @Test
    public void testDiffEx() throws Exception {
        //noinspection unchecked
        ResponseDataObject response = service.datasetDiffAnalysis( // Params:
                DatasetArg.valueOf( String.valueOf( ees.get( 1 ).getId() ) ), // Required
                IntArg.valueOf( "0" ), IntArg.valueOf( "1" ), new MockHttpServletResponse() );

        assertNotNull( response.getData() );
        assertTrue( response.getData() instanceof Collection<?> );
        // since we are using mock ees, the diffex array will likely be empty.
    }

    @Test
    public void testAnnotations() throws Exception {
        //noinspection unchecked
        ResponseDataObject response = service
                .datasetAnnotations( DatasetArg.valueOf( String.valueOf( ees.get( 1 ).getId() ) ),
                        new MockHttpServletResponse() );

        assertNotNull( response.getData() );
        assertTrue( response.getData() instanceof Collection<?> );
        // since we are using mock ees, the annotations array will likely be empty.
    }
}
