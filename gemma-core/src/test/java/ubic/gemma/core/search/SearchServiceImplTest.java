package ubic.gemma.core.search;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.test.util.ReflectionTestUtils;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit-level smoke tests for {@link SearchServiceImpl}.
 *
 * <p>Validates the Step-5 wiring with an "empty index": all registered {@link SearchSource}
 * beans return empty collections, and we assert that:
 * <ol>
 *   <li>no exception is thrown,</li>
 *   <li>the resulting {@link SearchService.SearchResultMap} is empty,</li>
 *   <li>{@link SearchService.SearchResultMap#getResultTypes()} is empty,</li>
 *   <li>VO-conversion bootstrap completes (the {@link ConversionService} is consulted but no
 *       hits are converted because none are returned).</li>
 * </ol>
 *
 * <p>This is the regression bar for "the search subsystem returns harmlessly when the Lucene
 * index has not yet been built." It does <em>not</em> exercise the Hibernate Search 7 backend
 * itself; that lives behind a Spring + JPA boot that the integration tests cover.
 *
 * @author paul
 */
public class SearchServiceImplTest {

    private SearchServiceImpl searchService;

    @BeforeEach
    public void setUp() {
        searchService = new SearchServiceImpl();

        // ConversionService that pretends every supported (entity -> VO) and (Long -> VO) edge is
        // wired. SearchServiceImpl.afterPropertiesSet() asserts these via Assert.isTrue; without a
        // permissive stub the bootstrap fails before any search path runs.
        ConversionService conversionService = mock( ConversionService.class );
        when( conversionService.canConvert( any( Class.class ), any( Class.class ) ) ).thenReturn( true );
        when( conversionService.canConvert( any( TypeDescriptor.class ), any( TypeDescriptor.class ) ) ).thenReturn( true );

        // Empty SearchSource list models the "no backends registered" / "empty index" case.
        ReflectionTestUtils.setField( searchService, "searchSources", Collections.<SearchSource>emptyList() );
        ReflectionTestUtils.setField( searchService, "valueObjectConversionService", conversionService );

        // Triggers initializeSupportedResultTypes() + assembles the (empty) CompositeSearchSource.
        searchService.afterPropertiesSet();
    }

    @Test
    public void searchEmptyIndexReturnsEmptyMap() {
        SearchSettings settings = SearchSettings.expressionExperimentSearch( "anything" );

        SearchService.SearchResultMap[] result = new SearchService.SearchResultMap[1];
        assertThatNoException().isThrownBy( () -> result[0] = searchService.search( settings, new SearchContext( null, null ) ) );

        assertThat( result[0] ).isNotNull();
        assertThat( result[0].toList() ).isEmpty();
        assertThat( result[0].getResultTypes() ).isEmpty();
        assertThat( result[0].getByResultType( ExpressionExperiment.class ) ).isEmpty();
        assertThat( result[0].<Gene>getByResultObjectType( Gene.class ) ).isEmpty();
    }

    @Test
    public void searchEmptyQueryReturnsEmptyMap() throws SearchException {
        // Blank query short-circuits before any source is consulted.
        SearchSettings settings = SearchSettings.builder()
                .query( "   " )
                .resultType( ExpressionExperiment.class )
                .build();
        SearchService.SearchResultMap result = searchService.search( settings, new SearchContext( null, null ) );

        assertThat( result.toList() ).isEmpty();
        assertThat( result.getResultTypes() ).isEmpty();
    }

    @Test
    public void supportedResultTypesIsTheCanonicalEight() {
        // The supported-result-types contract is part of the OpenAPI surface: SearchWebService
        // reflects this set out as the resultTypes enum, so a regression here breaks /search.
        Set<Class<? extends Identifiable>> types = searchService.getSupportedResultTypes();
        assertThat( types ).hasSize( 9 ); // 8 historical roots + BlacklistedEntity
        assertThat( types ).contains( ExpressionExperiment.class, Gene.class );
    }

    @Test
    public void loadValueObjectOfEmptyResultListIsEmpty() {
        assertThat( searchService.loadValueObjects( Collections.<SearchResult<?>>emptyList() ) ).isEmpty();
    }
}
