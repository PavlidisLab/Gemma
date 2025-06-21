package ubic.gemma.rest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.core.util.test.category.SlowTest;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.rest.util.BaseJerseyIntegrationTest;
import ubic.gemma.rest.util.ResponseDataObject;
import ubic.gemma.rest.util.ResponseErrorObject;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.rest.util.Assertions.assertThat;

/**
 * @author tesarst
 */
@Category(SlowTest.class)
public class DatasetsRestTest extends BaseJerseyIntegrationTest {

    @Autowired
    private DatasetsWebService datasetsWebService;

    @Autowired
    private DatasetArgService datasetArgService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private PersistentDummyObjectHelper testHelper;

    /* fixtures */
    private final ArrayList<ExpressionExperiment> ees = new ArrayList<>( 10 );

    @Before
    public void setUpMocks() {
        for ( int i = 0; i < 10; i++ ) {
            testHelper.resetSeed();
            ees.add( testHelper.getTestExpressionExperimentWithAllDependencies( false ) );
        }
    }

    @After
    public void resetMocks() {
        expressionExperimentService.remove( ees );
    }

    @Test
    public void testAll() {
        DatasetsWebService.QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<DatasetsWebService.ExpressionExperimentWithSearchResultValueObject> response = datasetsWebService
                .getDatasets( null, FilterArg.valueOf( "" ), OffsetArg.valueOf( "5" ), LimitArg.valueOf( "5" ),
                        SortArg.valueOf( "+id" ) );
        assertThat( response )
                .hasFieldOrPropertyWithValue( "query", null )
                .hasFieldOrPropertyWithValue( "offset", 5 )
                .hasFieldOrPropertyWithValue( "limit", 5 )
                .hasFieldOrProperty( "totalElements" ); // FIXME: cannot test because of leftovers from other tests but should be 10
        assertThat( response.getData() )
                .isNotNull()
                .hasSize( 5 );
    }

    @Test
    public void testSomeByShortName() {
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = datasetsWebService.getDatasetsByIds( DatasetArrayArg.valueOf(
                        ees.get( 0 ).getShortName() + ", BAD_NAME, " + ees.get( 2 )
                                .getShortName() ), FilterArg.valueOf( "" ), OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ), SortArg.valueOf( "+id" ) );
        ExpressionExperiment ee = ees.get( 0 );
        assertThat( ee ).isNotNull();
        assertThat( ee.getAccession() ).isNotNull();
        assertThat( response.getData() )
                .isNotNull()
                .extracting( "id" )
                .containsExactlyInAnyOrder( ees.get( 0 ).getId(), ees.get( 2 ).getId() );
    }

    @Test
    public void testSomeById() {
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = datasetsWebService.getDatasetsByIds( DatasetArrayArg.valueOf(
                        ees.get( 0 ).getId() + ", 12310, " + ees.get( 2 )
                                .getId() ), FilterArg.valueOf( "" ), OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ), SortArg.valueOf( "+id" ) );
        ExpressionExperiment ee = ees.get( 0 );
        assertThat( ee ).isNotNull();
        assertThat( ee.getAccession() ).isNotNull();
        assertThat( response.getData() )
                .isNotNull()
                .extracting( "id" )
                .containsExactlyInAnyOrder( ees.get( 0 ).getId(), ees.get( 2 ).getId() );
    }

    @Test
    public void testAllFilterById() {
        DatasetsWebService.QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<DatasetsWebService.ExpressionExperimentWithSearchResultValueObject> response = datasetsWebService.getDatasets(
                null,
                FilterArg.valueOf( "id = " + ees.get( 0 ).getId() ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" )
        );
        assertThat( response.getData() )
                .isNotNull()
                .hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "id", ees.get( 0 ).getId() );
    }

    @Test
    public void testAllFilterByIdIn() {
        FilterArg<ExpressionExperiment> filterArg = FilterArg.valueOf( "id in (" + ees.get( 0 ).getId() + ")" );
        assertThat( datasetArgService.getFilters( filterArg ) )
                .extracting( of -> of.get( 0 ) )
                .first()
                .hasFieldOrPropertyWithValue( "objectAlias", ExpressionExperimentDao.OBJECT_ALIAS )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", Collections.singletonList( ees.get( 0 ).getId() ) );
        DatasetsWebService.QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<DatasetsWebService.ExpressionExperimentWithSearchResultValueObject> response = datasetsWebService.getDatasets(
                null,
                filterArg,
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" )
        );
        assertThat( response.getData() )
                .isNotNull()
                .hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "id", ees.get( 0 ).getId() )
                .hasFieldOrPropertyWithValue( "shortName", ees.get( 0 ).getShortName() );
    }

    @Test
    public void testAllFilterByShortName() {
        FilterArg<ExpressionExperiment> filterArg = FilterArg.valueOf( "shortName = " + ees.get( 0 ).getShortName() );
        assertThat( datasetArgService.getFilters( filterArg ) )
                .extracting( of -> of.get( 0 ) )
                .first()
                .hasFieldOrPropertyWithValue( "objectAlias", ExpressionExperimentDao.OBJECT_ALIAS )
                .hasFieldOrPropertyWithValue( "propertyName", "shortName" )
                .hasFieldOrPropertyWithValue( "requiredValue", ees.get( 0 ).getShortName() );
        DatasetsWebService.QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<DatasetsWebService.ExpressionExperimentWithSearchResultValueObject> response = datasetsWebService.getDatasets(
                null,
                filterArg,
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" )
        );
        assertThat( response.getData() )
                .isNotNull()
                .hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "id", ees.get( 0 ).getId() )
                .hasFieldOrPropertyWithValue( "shortName", ees.get( 0 ).getShortName() );
    }

    @Test
    public void testAllFilterByShortNameIn() {
        FilterArg<ExpressionExperiment> filterArg = FilterArg.valueOf( "shortName in (" + ees.get( 0 ).getShortName() + ")" );
        assertThat( datasetArgService.getFilters( filterArg ) )
                .extracting( of -> of.get( 0 ) )
                .first()
                .hasFieldOrPropertyWithValue( "objectAlias", ExpressionExperimentDao.OBJECT_ALIAS )
                .hasFieldOrPropertyWithValue( "propertyName", "shortName" )
                .hasFieldOrPropertyWithValue( "requiredValue", Collections.singletonList( ees.get( 0 ).getShortName() ) );
        DatasetsWebService.QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<DatasetsWebService.ExpressionExperimentWithSearchResultValueObject> response = datasetsWebService.getDatasets(
                null,
                filterArg,
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" )
        );
        assertThat( response.getData() )
                .isNotNull()
                .hasSize( 1 )
                .first()
                .hasFieldOrPropertyWithValue( "id", ees.get( 0 ).getId() )
                .hasFieldOrPropertyWithValue( "shortName", ees.get( 0 ).getShortName() );
    }

    @Test
    public void testAllFilterByIdInOrShortNameIn() {
        FilterArg<ExpressionExperiment> filterArg = FilterArg.valueOf( "id in (" + ees.get( 0 ).getId() + ") or shortName in (" + ees.get( 1 ).getShortName() + ")" );
        assertThat( datasetArgService.getFilters( filterArg ) )
                .hasSize( 1 );
        /*
        assertThat( filterArg.getFilters( expressionExperimentService ).get( 0 ) )
                .hasSize( 2 );
        assertThat( filterArg.getFilters( expressionExperimentService ).get( 0 )[0] )
                .hasFieldOrPropertyWithValue( "objectAlias", ExpressionExperimentDao.OBJECT_ALIAS )
                .hasFieldOrPropertyWithValue( "propertyName", "id" )
                .hasFieldOrPropertyWithValue( "requiredValue", Collections.singletonList( ees.get( 0 ).getId() ) );
        assertThat( filterArg.getFilters( expressionExperimentService ).get( 0 )[1] )
                .hasFieldOrPropertyWithValue( "objectAlias", ExpressionExperimentDao.OBJECT_ALIAS )
                .hasFieldOrPropertyWithValue( "propertyName", "shortName" )
                .hasFieldOrPropertyWithValue( "requiredValue", Collections.singletonList( ees.get( 1 ).getShortName() ) );
         */
        DatasetsWebService.QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<DatasetsWebService.ExpressionExperimentWithSearchResultValueObject> response = datasetsWebService.getDatasets(
                null,
                filterArg,
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" )
        );
        assertThat( response.getData() )
                .isNotNull()
                .hasSize( 2 );
        assertThat( response.getData() )
                .extracting( "id" )
                .containsExactlyInAnyOrder( ees.get( 0 ).getId(), ees.get( 1 ).getId() );
    }

    @Test
    public void testAllWithTooLargeLimit() {
        assertThat( target( "/datasets" ).queryParam( "limit", "101" ).request().get() )
                .hasStatus( Response.Status.BAD_REQUEST )
                .entityAs( ResponseErrorObject.class )
                .extracting( ResponseErrorObject::getError )
                .satisfies( err -> {
                    assertThat( err.getCode() ).isEqualTo( 400 );
                    assertThat( err.getMessage() ).isEqualTo( "The provided limit cannot exceed 100." );
                } );
    }

    @Test
    public void testFilterByGeeqQualityScore() {
        assertThat( target( "/datasets" ).queryParam( "filter", "geeq.publicQualityScore <= 1.0" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_JSON_TYPE );
    }

    @Test
    public void testGetDatasetRawExpression() {
        ExpressionExperiment ee = ees.get( 0 );
        assertThat( target( "/datasets/" + ee.getId() + "/data/raw" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( new MediaType( "text", "tab-separated-values" ) )
                .hasHeaderSatisfying( "Content-Disposition", values -> {
                    assertThat( values ).singleElement().asString().endsWith( "_expmat.unfilt.data.txt\"" );
                } )
                .hasLength()
                .entityAsStream()
                .asString( StandardCharsets.UTF_8 )
                .contains( ee.getShortName() )
                // there's 7 comment lines, 1 header and then one line per raw EV (there are two platforms the default collection size in the fixture)
                .hasLineCount( 13 + 2 * testHelper.getTestElementCollectionSize() );

        // as a download, the Content-Encoding is not set and the .gz extension is kept, the payload also remains
        // compressed
        assertThat( target( "/datasets/" + ee.getId() + "/data/raw" ).queryParam( "download", "true" ).request().get() )
                .hasStatus( Response.Status.OK )
                .hasMediaTypeCompatibleWith( MediaType.APPLICATION_OCTET_STREAM_TYPE )
                .hasLength()
                .hasHeaderSatisfying( "Content-Disposition", values -> {
                    assertThat( values ).singleElement().asString().endsWith( "_expmat.unfilt.data.txt.gz\"" );
                } )
                .entityAsStream()
                .satisfies( stream -> {
                    assertThat( new GZIPInputStream( stream ) )
                            .asString( StandardCharsets.UTF_8 )
                            .contains( ee.getShortName() )
                            // there's 7 comment lines, 1 header and then one line per raw EV (there are two platforms the default collection size in the fixture)
                            .hasLineCount( 13 + 2 * testHelper.getTestElementCollectionSize() );
                } );
    }
}
