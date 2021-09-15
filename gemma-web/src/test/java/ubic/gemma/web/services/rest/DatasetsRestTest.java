package ubic.gemma.web.services.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.web.services.rest.util.PaginatedResponseDataObject;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.args.DatasetArrayArg;
import ubic.gemma.web.services.rest.util.args.DatasetFilterArg;
import ubic.gemma.web.services.rest.util.args.IntArg;
import ubic.gemma.web.services.rest.util.args.SortArg;
import ubic.gemma.web.util.BaseSpringWebTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author tesarst
 */
public class DatasetsRestTest extends BaseSpringWebTest {

    @Autowired
    private DatasetsWebService datasetsWebService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /* fixtures */
    private ArrayList<ExpressionExperiment> ees = new ArrayList<>( 10 );

    @Before
    public void setUp() {
        // FIXME: this should not be necessary, but other tests are not cleaning up their fixtures
        expressionExperimentService.remove( expressionExperimentService.loadAll() );
        for ( int i = 0; i < 10; i++ ) {
            ees.add( this.getNewTestPersistentCompleteExpressionExperiment() );
        }
    }

    @After
    public void tearDown() {
        expressionExperimentService.remove( ees );
    }

    @Test
    public void testAll() {
        PaginatedResponseDataObject<ExpressionExperimentValueObject> response = datasetsWebService
                .all( DatasetFilterArg.valueOf( "" ), IntArg.valueOf( "5" ), IntArg.valueOf( "5" ),
                        SortArg.valueOf( "+id" ), new MockHttpServletResponse() );
        assertThat( response )
                .hasFieldOrPropertyWithValue( "offset", 5 )
                .hasFieldOrPropertyWithValue( "limit", 5 )
                .hasFieldOrPropertyWithValue( "totalElements", 10L );
        assertThat( response.getData() )
                .isNotNull()
                .asList().hasSize( 5 );
    }

    @Test
    public void testSome() {
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = datasetsWebService.datasets( DatasetArrayArg.valueOf(
                        ees.get( 0 ).getShortName() + ", BAD_NAME, " + ees.get( 2 )
                                .getShortName() ), DatasetFilterArg.valueOf( "" ), IntArg.valueOf( "0" ),
                IntArg.valueOf( "10" ), SortArg.valueOf( "+id" ), new MockHttpServletResponse() );
        assertThat( response.getData() )
                .isNotNull()
                .asList().hasSize( 2 )
                .first()
                .hasFieldOrPropertyWithValue( "accession", ees.get( 0 ).getAccession().getAccession() )
                .hasFieldOrPropertyWithValue( "externalDatabase", ees.get( 0 ).getAccession().getExternalDatabase().getName() )
                .hasFieldOrPropertyWithValue( "externalUri", ees.get( 0 ).getAccession().getExternalDatabase().getWebUri() );
    }

    @Test
    public void testAllFilterById() {
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = datasetsWebService.all(
                DatasetFilterArg.valueOf( "id = " + ees.get( 0 ).getId() ),
                IntArg.valueOf( "0" ),
                IntArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                new MockHttpServletResponse() );
        assertThat( response.getData() )
                .isNotNull()
                .asList().hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "id", ees.get( 0 ).getId() );
    }

    @Test
    public void testAllFilterByShortName() {
        DatasetFilterArg filterArg = DatasetFilterArg.valueOf( "shortName = " + ees.get( 0 ).getShortName() );
        assertThat( filterArg.getObjectFilters().get( 0 )[0] )
                .hasFieldOrPropertyWithValue( "objectAlias", ObjectFilter.DAO_EE_ALIAS )
                .hasFieldOrPropertyWithValue( "propertyName", "shortName" )
                .hasFieldOrPropertyWithValue( "requiredValue", ees.get( 0 ).getShortName() );
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = datasetsWebService.all(
                filterArg,
                IntArg.valueOf( "0" ),
                IntArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                new MockHttpServletResponse() );
        assertThat( response.getData() )
                .isNotNull()
                .asList().hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "id", ees.get( 0 ).getId() )
                .hasFieldOrPropertyWithValue( "shortName", ees.get( 0 ).getShortName() );
    }
}
