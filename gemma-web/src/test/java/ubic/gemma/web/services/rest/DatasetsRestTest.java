package ubic.gemma.web.services.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.services.rest.util.MalformedArgException;
import ubic.gemma.web.services.rest.util.PaginatedResponseDataObject;
import ubic.gemma.web.services.rest.util.ResponseDataObject;
import ubic.gemma.web.services.rest.util.args.*;
import ubic.gemma.web.util.BaseSpringWebTest;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
                .all( FilterArg.valueOf( "" ), OffsetArg.valueOf( "5" ), LimitArg.valueOf( "5" ),
                        SortArg.valueOf( "+id" ), new MockHttpServletResponse() );
        assertThat( response )
                .hasFieldOrPropertyWithValue( "offset", 5 )
                .hasFieldOrPropertyWithValue( "limit", 5 )
                .hasFieldOrProperty( "totalElements" ); // FIXME: cannot test because of leftovers from other tests but should be 10
        assertThat( response.getData() )
                .isNotNull()
                .asList().hasSize( 5 );
    }

    @Test
    public void testSome() {
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = datasetsWebService.datasets( DatasetArrayArg.valueOf(
                        ees.get( 0 ).getShortName() + ", BAD_NAME, " + ees.get( 2 )
                                .getShortName() ), FilterArg.valueOf( "" ), OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ), SortArg.valueOf( "+id" ), new MockHttpServletResponse() );
        assertThat( response.getData() )
                .isNotNull()
                .asList().hasSize( 2 )
                .first()
                .hasFieldOrPropertyWithValue( "accession", ees.get( 0 ).getAccession().getAccession() )
                .hasFieldOrPropertyWithValue( "externalDatabase", ees.get( 0 ).getAccession().getExternalDatabase().getName() )
                .hasFieldOrPropertyWithValue( "externalUri", ees.get( 0 ).getAccession().getExternalDatabase().getWebUri() );
    }

    @Test
    @Category(SlowTest.class)
    public void testSomeById() {
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = datasetsWebService.datasets( DatasetArrayArg.valueOf(
                        ees.get( 0 ).getId() + ", 12310, " + ees.get( 2 )
                                .getId() ), FilterArg.valueOf( "" ), OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ), SortArg.valueOf( "+id" ), new MockHttpServletResponse() );
        assertThat( response.getData() )
                .isNotNull()
                .asList().hasSize( 2 )
                .first()
                .hasFieldOrPropertyWithValue( "accession", ees.get( 0 ).getAccession().getAccession() )
                .hasFieldOrPropertyWithValue( "externalDatabase", ees.get( 0 ).getAccession().getExternalDatabase().getName() )
                .hasFieldOrPropertyWithValue( "externalUri", ees.get( 0 ).getAccession().getExternalDatabase().getWebUri() )
                .hasFieldOrPropertyWithValue( "taxon", ees.get( 0 ).getTaxon().getCommonName() )
                .hasFieldOrPropertyWithValue( "taxonId", ees.get( 0 ).getTaxon().getId() );
    }

    @Test
    @Category(SlowTest.class)
    public void testAllFilterById() {
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = datasetsWebService.all(
                FilterArg.valueOf( "id = " + ees.get( 0 ).getId() ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                new MockHttpServletResponse() );
        assertThat( response.getData() )
                .isNotNull()
                .asList().hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "id", ees.get( 0 ).getId() );
    }

    @Test
    @Category(SlowTest.class)
    public void testAllFilterByIdIn() {
        FilterArg filterArg = FilterArg.valueOf( "id in (" + ees.get( 0 ).getId() + ")" );
        assertThat( filterArg.getObjectFilters( expressionExperimentService ) )
                .extracting( of -> of[0] )
                .first()
                .hasFieldOrPropertyWithValue( "objectAlias", ExpressionExperimentDao.OBJECT_ALIAS )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", Collections.singletonList( ees.get( 0 ).getId() ) );
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = datasetsWebService.all(
                filterArg,
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                new MockHttpServletResponse() );
        assertThat( response.getData() )
                .isNotNull()
                .asList().hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "id", ees.get( 0 ).getId() )
                .hasFieldOrPropertyWithValue( "shortName", ees.get( 0 ).getShortName() );
    }

    @Test
    @Category(SlowTest.class)
    public void testAllFilterByShortName() {
        FilterArg filterArg = FilterArg.valueOf( "shortName = " + ees.get( 0 ).getShortName() );
        assertThat( filterArg.getObjectFilters( expressionExperimentService ) )
                .extracting( of -> of[0] )
                .first()
                .hasFieldOrPropertyWithValue( "objectAlias", ExpressionExperimentDao.OBJECT_ALIAS )
                .hasFieldOrPropertyWithValue( "propertyName", "shortName" )
                .hasFieldOrPropertyWithValue( "requiredValue", ees.get( 0 ).getShortName() );
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = datasetsWebService.all(
                filterArg,
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                new MockHttpServletResponse() );
        assertThat( response.getData() )
                .isNotNull()
                .asList().hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "id", ees.get( 0 ).getId() )
                .hasFieldOrPropertyWithValue( "shortName", ees.get( 0 ).getShortName() );
    }

    @Test
    @Category(SlowTest.class)
    public void testAllFilterByShortNameIn() {
        FilterArg filterArg = FilterArg.valueOf( "shortName in (" + ees.get( 0 ).getShortName() + ")" );
        assertThat( filterArg.getObjectFilters( expressionExperimentService ) )
                .extracting( of -> of[0] )
                .first()
                .hasFieldOrPropertyWithValue( "objectAlias", ExpressionExperimentDao.OBJECT_ALIAS )
                .hasFieldOrPropertyWithValue( "propertyName", "shortName" )
                .hasFieldOrPropertyWithValue( "requiredValue", Collections.singletonList( ees.get( 0 ).getShortName() ) );
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = datasetsWebService.all(
                filterArg,
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                new MockHttpServletResponse() );
        assertThat( response.getData() )
                .isNotNull()
                .asList().hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "id", ees.get( 0 ).getId() )
                .hasFieldOrPropertyWithValue( "shortName", ees.get( 0 ).getShortName() );
    }

    @Test
    @Category(SlowTest.class)
    public void testAllFilterByIdInOrShortNameIn() {
        FilterArg filterArg = FilterArg.valueOf( "id in (" + ees.get( 0 ).getId() + ") or shortName in (" + ees.get( 1 ).getShortName() + ")" );
        assertThat( filterArg.getObjectFilters( expressionExperimentService ) )
                .hasSize( 1 );
        /*
        assertThat( filterArg.getObjectFilters( expressionExperimentService ).get( 0 ) )
                .hasSize( 2 );
        assertThat( filterArg.getObjectFilters( expressionExperimentService ).get( 0 )[0] )
                .hasFieldOrPropertyWithValue( "objectAlias", ExpressionExperimentDao.OBJECT_ALIAS )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", Collections.singletonList( ees.get( 0 ).getId() ) );
        assertThat( filterArg.getObjectFilters( expressionExperimentService ).get( 0 )[1] )
                .hasFieldOrPropertyWithValue( "objectAlias", ExpressionExperimentDao.OBJECT_ALIAS )
                .hasFieldOrPropertyWithValue( "propertyName", "shortName" )
                .hasFieldOrPropertyWithValue( "requiredValue", Collections.singletonList( ees.get( 1 ).getShortName() ) );
         */
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = datasetsWebService.all(
                filterArg,
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                new MockHttpServletResponse() );
        assertThat( response.getData() )
                .isNotNull()
                .asList().hasSize( 2 );
        assertThat( response.getData() )
                .extracting( "id" )
                .containsExactlyInAnyOrder( ees.get( 0 ).getId(), ees.get( 1 ).getId() );
    }

    @Test
    public void testAllWithTooLargeLimit() {
        assertThatThrownBy( () -> {
            datasetsWebService.all( FilterArg.valueOf( "" ),
                    OffsetArg.valueOf( "0" ),
                    LimitArg.valueOf( "101" ),
                    SortArg.valueOf( "+id" ),
                    new MockHttpServletResponse() );
        } ).isInstanceOf( MalformedArgException.class );
    }

    @Test
    @Category(SlowTest.class)
    public void testFilterByGeeqPublicationScore() {
        datasetsWebService.all( FilterArg.valueOf( "geeq.sScorePublication <= 1.0" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" ),
                new MockHttpServletResponse() );
    }

    @Test
    public void testGetDatasetRawExpression() throws IOException {
        ExpressionExperiment ee = ees.get( 0 );
        Response response = datasetsWebService.getDatasetRawExpression( DatasetArg.valueOf( String.valueOf( ee.getId() ) ) );
        byte[] payload;
        try ( ByteArrayOutputStream os = new ByteArrayOutputStream() ) {
            ( ( StreamingOutput ) response.getEntity() ).write( os );
            payload = os.toByteArray();
        }
        String decodedPayload = new String( payload, StandardCharsets.UTF_8 );
        // there's 7 comment lines, 1 header and then one line per raw EV (there are two platforms the default collection size in the fixture)
        assertThat( decodedPayload )
                .isNotEmpty()
                .contains( ee.getShortName() )
                .hasLineCount( 8 + 2 * testHelper.getTestElementCollectionSize() );
    }
}
