package ubic.gemma.web.services.rest;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.args.ArrayDatasetArg;
import ubic.gemma.web.services.rest.util.args.DatasetFilterArg;
import ubic.gemma.web.services.rest.util.args.IntArg;
import ubic.gemma.web.services.rest.util.args.SortArg;
import ubic.gemma.web.util.BaseSpringWebTest;

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
    private DatasetsWebService datasetsWebService;

    @Before
    public void setUp() {
        if ( DatasetsRestTest.loaded ) {
            return;
        }
        for ( int i = 0; i < 10; i++ ) {
            DatasetsRestTest.ees.add( this.getNewTestPersistentCompleteExpressionExperiment() );
        }
        DatasetsRestTest.loaded = true;
    }

    @Test
    public void testAll() {
        ResponseDataObject response = datasetsWebService
                .all( DatasetFilterArg.valueOf( "" ), IntArg.valueOf( "5" ), IntArg.valueOf( "5" ),
                        SortArg.valueOf( "+id" ), new MockHttpServletResponse() );

        assertNotNull( response.getData() );
        assertTrue( response.getData() instanceof Collection<?> );
        assertEquals( 5, ( ( Collection ) response.getData() ).size() );
        assertTrue(
                ( ( Collection ) response.getData() ).iterator().next() instanceof ExpressionExperimentValueObject );
    }

    @Test
    public void testSome() {
        ResponseDataObject response = datasetsWebService.datasets( ArrayDatasetArg.valueOf(
                DatasetsRestTest.ees.get( 0 ).getShortName() + ", BAD_NAME, " + DatasetsRestTest.ees.get( 2 )
                        .getShortName() ), DatasetFilterArg.valueOf( "" ), IntArg.valueOf( "0" ),
                IntArg.valueOf( "10" ), SortArg.valueOf( "+id" ), new MockHttpServletResponse() );

        assertNotNull( response.getData() );
        assertTrue( response.getData() instanceof Collection<?> );
        assertEquals( 2, ( ( Collection ) response.getData() ).size() );
        assertTrue(
                ( ( Collection ) response.getData() ).iterator().next() instanceof ExpressionExperimentValueObject );
    }
}
