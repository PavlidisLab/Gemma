package ubic.gemma.core.search;

import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    /**
     * Pins the sub-transaction template configuration: REQUIRES_NEW + readOnly.
     *
     * <p>Regression guard for the 2026-05-27 frink hit where {@code /search?query=STAT5B}
     * returned 500 ({@code UnexpectedRollbackException}) — an inner converter's
     * {@code LazyInitException} marked the OUTER search transaction rollback-only via the
     * shared-propagation @Transactional advisor, even though the entity-path catch+promote
     * had already produced valid id-path results. Running the brittle convert call in a
     * {@link TransactionDefinition#PROPAGATION_REQUIRES_NEW} sub-transaction contains the
     * rollback; this test prevents a future "simplification" from collapsing the sub-txn
     * back into the parent.
     */
    @Test
    public void entityPathTxTemplateUsesRequiresNewAndReadOnly() {
        SearchServiceImpl serviceWithTxManager = new SearchServiceImpl();
        ConversionService conversionService = mock( ConversionService.class );
        when( conversionService.canConvert( any( Class.class ), any( Class.class ) ) ).thenReturn( true );
        when( conversionService.canConvert( any( TypeDescriptor.class ), any( TypeDescriptor.class ) ) ).thenReturn( true );
        PlatformTransactionManager txManager = mock( PlatformTransactionManager.class );
        ReflectionTestUtils.setField( serviceWithTxManager, "searchSources", Collections.<SearchSource>emptyList() );
        ReflectionTestUtils.setField( serviceWithTxManager, "valueObjectConversionService", conversionService );
        ReflectionTestUtils.setField( serviceWithTxManager, "transactionManager", txManager );
        serviceWithTxManager.afterPropertiesSet();

        TransactionTemplate template = ( TransactionTemplate )
                ReflectionTestUtils.getField( serviceWithTxManager, "entityPathTxTemplate" );
        assertThat( template ).as( "tx template must be constructed when a tx manager is wired" )
                .isNotNull();
        assertThat( template.getPropagationBehavior() )
                .as( "must be REQUIRES_NEW so inner @Transactional rollback is contained" )
                .isEqualTo( TransactionDefinition.PROPAGATION_REQUIRES_NEW );
        assertThat( template.isReadOnly() ).isTrue();
    }

    /**
     * Behavioural fallback: when the entity-path converter throws
     * {@link ConversionFailedException} (the wrap around {@link LazyInitializationException}),
     * {@link SearchServiceImpl#loadValueObjects(Collection)} must catch it and fall through
     * to the id-path rather than propagating. This pairs with
     * {@link #entityPathTxTemplateUsesRequiresNewAndReadOnly()}: REQUIRES_NEW keeps the
     * outer txn clean; this guard ensures the catch+id-promote pipeline is still wired.
     */
    @Test
    public void entityPathFailurePromotesToIdPath() {
        SearchServiceImpl svc = new SearchServiceImpl();
        ConversionService conversionService = mock( ConversionService.class );
        when( conversionService.canConvert( any( Class.class ), any( Class.class ) ) ).thenReturn( true );
        when( conversionService.canConvert( any( TypeDescriptor.class ), any( TypeDescriptor.class ) ) ).thenReturn( true );

        // Make a TransactionTemplate that uses an in-line execution path so we can drive
        // through afterPropertiesSet without spinning a real tx manager.
        PlatformTransactionManager txManager = mock( PlatformTransactionManager.class );
        when( txManager.getTransaction( any() ) ).thenReturn( new SimpleTransactionStatus() );

        // Entity-collection convert throws (simulates the LazyInit-AuditEvent shape).
        // Id-collection convert succeeds and returns a single VO.
        ExpressionExperiment ee = new ExpressionExperiment();
        ReflectionTestUtils.setField( ee, "id", 42L );
        ExpressionExperimentValueObject vo = new ExpressionExperimentValueObject( 42L );
        when( conversionService.convert( any( Collection.class ),
                argThat( ( TypeDescriptor td ) -> td != null && td.getElementTypeDescriptor() != null
                        && ExpressionExperiment.class.equals( td.getElementTypeDescriptor().getType() ) ),
                any( TypeDescriptor.class ) ) )
                .thenThrow( new ConversionFailedException(
                        TypeDescriptor.valueOf( ExpressionExperiment.class ),
                        TypeDescriptor.valueOf( ExpressionExperimentValueObject.class ),
                        Arrays.asList( ee ),
                        new LazyInitializationException( "Could not initialize proxy [AuditEvent#1] - no session" ) ) );
        when( conversionService.convert( any( Collection.class ),
                argThat( ( TypeDescriptor td ) -> td != null && td.getElementTypeDescriptor() != null
                        && Long.class.equals( td.getElementTypeDescriptor().getType() ) ),
                any( TypeDescriptor.class ) ) )
                .thenReturn( new ArrayList<>( Collections.singletonList( vo ) ) );

        ReflectionTestUtils.setField( svc, "searchSources", Collections.<SearchSource>emptyList() );
        ReflectionTestUtils.setField( svc, "valueObjectConversionService", conversionService );
        ReflectionTestUtils.setField( svc, "transactionManager", txManager );
        svc.afterPropertiesSet();

        SearchResult<ExpressionExperiment> sr = SearchResult.from( ExpressionExperiment.class, ee, 1.0, null, "test" );
        List<SearchResult<? extends IdentifiableValueObject<?>>> out =
                svc.loadValueObjects( Collections.<SearchResult<?>>singletonList( sr ) );

        // The fallback id-path produced our VO; the LazyInit-wrapped failure was swallowed.
        assertThat( out ).hasSize( 1 );
        assertThat( out.get( 0 ).getResultObject() ).isSameAs( vo );

        // And the sub-txn template was actually invoked: getTransaction was called with
        // REQUIRES_NEW propagation. Without this, the brittle convert would have run in the
        // outer (shared) txn and re-introduced the rollback-poisoning regression.
        verify( txManager ).getTransaction( argThat(
                ( TransactionDefinition def ) -> def != null
                        && def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW
                        && def.isReadOnly() ) );
    }

    /**
     * When no PlatformTransactionManager is wired (existing unit-test contexts), the entity
     * path falls back to an inline convert. The catch+id-promote still applies.
     */
    @Test
    public void entityPathFallsBackToInlineWhenNoTxManager() {
        // The default setUp() wired no tx manager. Drive a single entity through the
        // entity-path with a converter that throws — should NOT propagate the exception
        // (the catch + id-path fallback path is unaffected by tx manager absence).
        ConversionService cs = ( ConversionService ) ReflectionTestUtils.getField( searchService, "valueObjectConversionService" );
        ExpressionExperiment ee = new ExpressionExperiment();
        ReflectionTestUtils.setField( ee, "id", 7L );
        ExpressionExperimentValueObject vo = new ExpressionExperimentValueObject( 7L );
        when( cs.convert( any( Collection.class ),
                argThat( ( TypeDescriptor td ) -> td != null && td.getElementTypeDescriptor() != null
                        && ExpressionExperiment.class.equals( td.getElementTypeDescriptor().getType() ) ),
                any( TypeDescriptor.class ) ) )
                .thenThrow( new ConversionFailedException(
                        TypeDescriptor.valueOf( ExpressionExperiment.class ),
                        TypeDescriptor.valueOf( ExpressionExperimentValueObject.class ),
                        Arrays.asList( ee ),
                        new LazyInitializationException( "boom" ) ) );
        when( cs.convert( any( Collection.class ),
                argThat( ( TypeDescriptor td ) -> td != null && td.getElementTypeDescriptor() != null
                        && Long.class.equals( td.getElementTypeDescriptor().getType() ) ),
                any( TypeDescriptor.class ) ) )
                .thenReturn( new ArrayList<>( Collections.singletonList( vo ) ) );

        SearchResult<ExpressionExperiment> sr = SearchResult.from( ExpressionExperiment.class, ee, 1.0, null, "test" );
        assertThatNoException().isThrownBy( () ->
                searchService.loadValueObjects( Collections.<SearchResult<?>>singletonList( sr ) ) );
    }
}
