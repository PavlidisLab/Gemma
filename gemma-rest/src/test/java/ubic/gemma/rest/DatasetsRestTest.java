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
import ubic.gemma.rest.util.*;
import ubic.gemma.rest.util.args.*;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        DatasetsWebService.QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<List<DatasetsWebService.ExpressionExperimentWithSearchResultValueObject>> response = datasetsWebService
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
    public void testSome() {
        ResponseDataObject<List<ExpressionExperimentValueObject>> response = datasetsWebService.getDatasetsByIds( DatasetArrayArg.valueOf(
                        ees.get( 0 ).getShortName() + ", BAD_NAME, " + ees.get( 2 )
                                .getShortName() ), FilterArg.valueOf( "" ), OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ), SortArg.valueOf( "+id" ) );
        ExpressionExperiment ee = ees.get( 0 );
        assertThat( ee ).isNotNull();
        assertThat( ee.getAccession() ).isNotNull();
        assertThat( response.getData() )
                .isNotNull()
                .hasSize( 2 )
                .first()
                .hasFieldOrPropertyWithValue( "accession", ee.getAccession().getAccession() )
                .hasFieldOrPropertyWithValue( "externalDatabase", ee.getAccession().getExternalDatabase().getName() )
                .hasFieldOrPropertyWithValue( "externalUri", ee.getAccession().getExternalDatabase().getWebUri() );
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
                .hasSize( 2 )
                .first()
                .hasFieldOrPropertyWithValue( "accession", ee.getAccession().getAccession() )
                .hasFieldOrPropertyWithValue( "externalDatabase", ee.getAccession().getExternalDatabase().getName() )
                .hasFieldOrPropertyWithValue( "externalUri", ee.getAccession().getExternalDatabase().getWebUri() )
                .hasFieldOrPropertyWithValue( "taxon", ee.getTaxon().getCommonName() )
                .hasFieldOrPropertyWithValue( "taxonId", ee.getTaxon().getId() );
    }

    @Test
    public void testAllFilterById() {
        DatasetsWebService.QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<List<DatasetsWebService.ExpressionExperimentWithSearchResultValueObject>> response = datasetsWebService.getDatasets(
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
        DatasetsWebService.QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<List<DatasetsWebService.ExpressionExperimentWithSearchResultValueObject>> response = datasetsWebService.getDatasets(
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
        DatasetsWebService.QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<List<DatasetsWebService.ExpressionExperimentWithSearchResultValueObject>> response = datasetsWebService.getDatasets(
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
        DatasetsWebService.QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<List<DatasetsWebService.ExpressionExperimentWithSearchResultValueObject>> response = datasetsWebService.getDatasets(
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
        DatasetsWebService.QueriedAndFilteredAndInferredAndPaginatedResponseDataObject<List<DatasetsWebService.ExpressionExperimentWithSearchResultValueObject>> response = datasetsWebService.getDatasets(
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
        assertThatThrownBy( () -> {
            datasetsWebService.getDatasets(
                    null,
                    FilterArg.valueOf( "" ),
                    OffsetArg.valueOf( "0" ),
                    LimitArg.valueOf( "101" ),
                    SortArg.valueOf( "+id" )
            );
        } ).isInstanceOf( MalformedArgException.class );
    }

    @Test
    public void testFilterByGeeqQualityScore() {
        datasetsWebService.getDatasets(
                null,
                FilterArg.valueOf( "geeq.publicQualityScore <= 1.0" ),
                OffsetArg.valueOf( "0" ),
                LimitArg.valueOf( "10" ),
                SortArg.valueOf( "+id" )
        );
    }

    @Test
    public void testGetDatasetRawExpression() throws IOException {
        ExpressionExperiment ee = ees.get( 0 );
        Response response = datasetsWebService.getDatasetRawExpression( DatasetArg.valueOf( String.valueOf( ee.getId() ) ), null );
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
