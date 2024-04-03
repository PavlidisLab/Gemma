package ubic.gemma.core.search;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.annotation.reference.BibliographicReferenceService;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.expression.BlacklistedEntity;
import ubic.gemma.model.expression.BlacklistedValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.arrayDesign.BlacklistedPlatform;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.BlacklistedExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.BlacklistedEntityService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.ServiceBasedValueObjectConverter;
import ubic.gemma.persistence.util.TestComponent;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.*;

/**
 * Test conversion to VOs for search results.
 * <p>
 * The conversion is typically performed with a {@link ServiceBasedValueObjectConverter} which in turn
 * relies upon specific {@link ubic.gemma.persistence.service.BaseVoEnabledService} logic for performing the VO
 * conversion. Thus, we want to make sure that the service will produce the expected VOs.
 *
 * @author poirigui
 */
@ContextConfiguration
@TestExecutionListeners(WithSecurityContextTestExecutionListener.class)
public class SearchServiceVoConversionTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class SearchServiceVoConversionTestContextConfiguration extends SearchServiceTestContextConfiguration {

    }

    @Autowired
    private SearchService searchService;

    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private GeneSetService geneSetService;
    @Autowired
    private BibliographicReferenceService bibliographicReferenceService;
    @Autowired
    private BlacklistedEntityService blacklistedEntityService;

    /* fixtures */
    private ArrayDesign ad;
    private CompositeSequence cs;
    private ExpressionExperiment ee;
    private ExpressionExperimentValueObject eevo;
    private GeneSet gs;

    @Before
    public void setUp() {
        ad = new ArrayDesign();
        ad.setId( 11L );
        ad.setPrimaryTaxon( Taxon.Factory.newInstance( "Homo sapiens", "Human", 9606, false ) );
        cs = new CompositeSequence();
        cs.setId( 10L );
        cs.setArrayDesign( ad );
        ee = new ExpressionExperiment();
        ee.setId( 12L );
        eevo = new ExpressionExperimentValueObject();
        eevo.setId( 12L );
        gs = new GeneSet();
        gs.setId( 13L );
        when( arrayDesignService.loadValueObject( any( ArrayDesign.class ) ) ).thenAnswer( a -> new ArrayDesignValueObject( a.getArgument( 0, ArrayDesign.class ) ) );
        //noinspection unchecked
        when( arrayDesignService.loadValueObjects( anyCollection() ) ).thenAnswer( a -> ( ( Collection<ArrayDesign> ) a.getArgument( 0, Collection.class ) )
                .stream()
                .map( ArrayDesignValueObject::new )
                .collect( Collectors.toList() ) );
        when( compositeSequenceService.loadValueObject( any( CompositeSequence.class ) ) ).thenAnswer( a -> new CompositeSequenceValueObject( a.getArgument( 0, CompositeSequence.class ) ) );
        when( expressionExperimentService.loadValueObject( any( ExpressionExperiment.class ) ) ).thenAnswer( a -> new ExpressionExperimentValueObject( a.getArgument( 0, ExpressionExperiment.class ) ) );
        when( geneSetService.loadValueObject( any( GeneSet.class ) ) ).thenAnswer( a -> {
            GeneSet geneSet = a.getArgument( 0, GeneSet.class );
            return new DatabaseBackedGeneSetValueObject( geneSet, null, null );
        } );
    }

    @After
    public void tearDown() {
        reset( arrayDesignService, expressionExperimentService, geneSetService, compositeSequenceService, bibliographicReferenceService );
    }

    @Test
    @WithMockUser
    public void testConvertArrayDesign() {
        searchService.loadValueObject( SearchResult.from( ArrayDesign.class, ad, 1.0, null, "test object" ) );
        verify( arrayDesignService ).loadValueObject( ad );
    }

    @Test
    @WithMockUser
    public void testConvertArrayDesignCollection() {
        searchService.loadValueObjects( Collections.singleton( SearchResult.from( ArrayDesign.class, ad, 1.0, null, "test object" ) ) );
        verify( arrayDesignService ).loadValueObjects( Collections.singletonList( ad ) );
    }

    @Test
    public void testConvertBibliographicReference() {
        BibliographicReference br = new BibliographicReference();
        when( bibliographicReferenceService.loadValueObject( any( BibliographicReference.class ) ) )
                .thenAnswer( arg -> new BibliographicReferenceValueObject( arg.getArgument( 0, BibliographicReference.class ) ) );
        br.setId( 13L );
        searchService.loadValueObject( SearchResult.from( BibliographicReference.class, br, 1.0, null, "test object" ) );
        verify( bibliographicReferenceService ).loadValueObject( br );
    }

    @Test
    @WithMockUser
    public void testConvertCompositeSequence() {
        searchService.loadValueObject( SearchResult.from( CompositeSequence.class, cs, 1.0, null, "test object" ) );
        verify( compositeSequenceService ).loadValueObject( cs );
    }

    @Test
    @WithMockUser
    public void testConvertCompositeSequenceCollection() {
        when( compositeSequenceService.loadValueObjects( any() ) ).thenReturn( Collections.singletonList( new CompositeSequenceValueObject( cs ) ) );
        // this is a special case because of how it's implemented
        searchService.loadValueObjects( Collections.singleton( SearchResult.from( CompositeSequence.class, cs, 1.0, null, "test object" ) ) );
        verify( compositeSequenceService ).loadValueObjects( Collections.singletonList( cs ) );
    }

    @Test
    @WithMockUser
    public void testConvertExpressionExperiment() {
        searchService.loadValueObject( SearchResult.from( ExpressionExperiment.class, ee, 1.0, null, "test object" ) );
        verify( expressionExperimentService ).loadValueObject( ee );
    }

    @Test
    public void testConvertGeneSet() {
        // this is another complicated one because GeneSetService does not implement BaseVoEnabledService
        searchService.loadValueObject( SearchResult.from( GeneSet.class, gs, 1.0, null, "test object" ) );
        verify( geneSetService ).loadValueObject( gs );
    }

    @Test
    public void testConvertUninitializedResult() {
        DatabaseBackedGeneSetValueObject gsvo = new DatabaseBackedGeneSetValueObject( gs, new Taxon(), 1L );
        when( geneSetService.loadValueObjectById( 13L ) ).thenReturn( gsvo );
        SearchResult<IdentifiableValueObject<Identifiable>> sr = searchService.loadValueObject( SearchResult.from( GeneSet.class, 13L, 1.0, null, "test object" ) );
        assertThat( sr )
                .isNotNull()
                .hasFieldOrPropertyWithValue( "resultType", GeneSet.class )
                .hasFieldOrPropertyWithValue( "resultId", 13L )
                .hasFieldOrPropertyWithValue( "resultObject", gsvo )
                .hasFieldOrPropertyWithValue( "score", 1.0 )
                .hasFieldOrPropertyWithValue( "highlights", null );
        verify( geneSetService ).loadValueObjectById( 13L );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedResultTypeRaisesIllegalArgumentException() {
        ContrastResult cr = new ContrastResult();
        cr.setId( 1L );
        searchService.loadValueObject( SearchResult.from( ContrastResult.class, cr, 1.0, null, "test object" ) );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedResultTypeInCollectionRaisesIllegalArgumentException() {
        searchService.loadValueObjects( Collections.singleton( SearchResult.from( ContrastResult.class, new ContrastResult(), 0.0f, null, "test object" ) ) );
    }

    @Test
    public void testConvertAlreadyConvertedCollection() {
        searchService.loadValueObjects( Collections.singletonList(
                SearchResult.from( ExpressionExperiment.class, eevo, 0.0f, null, "test value object" ) ) );
        verify( expressionExperimentService ).loadValueObjectsByIds( Collections.singletonList( eevo.getId() ) );
    }

    @Test
    @WithMockUser
    public void testBlacklistedConversion() {
        BlacklistedEntity bp = new BlacklistedPlatform();
        bp.setId( 1L );
        bp.setShortName( "GPL123012" );
        BlacklistedEntity be = new BlacklistedExperiment();
        be.setId( 2L );
        be.setShortName( "GSE102930" );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        when( expressionExperimentService.loadValueObjects( any() ) ).thenReturn( Collections.singletonList( new ExpressionExperimentValueObject( ee ) ) );
        when( blacklistedEntityService.loadValueObjects( any() ) ).thenReturn( Arrays.asList( BlacklistedValueObject.fromEntity( bp ), BlacklistedValueObject.fromEntity( be ) ) );
        List<SearchResult<? extends IdentifiableValueObject<?>>> vos = searchService.loadValueObjects( Arrays.asList(
                SearchResult.from( BlacklistedEntity.class, be, 0.0, null, "test blacklisted object" ),
                SearchResult.from( BlacklistedEntity.class, bp, 0.0, null, "test blacklisted object" ),
                SearchResult.from( ExpressionExperiment.class, ee, 1.0, null, "test object" ) ) );
        verify( expressionExperimentService ).loadValueObjects( Collections.singletonList( ee ) );
        assertThat( vos )
                .extracting( "resultType", "resultId" )
                .containsExactlyInAnyOrder(
                        tuple( ExpressionExperiment.class, 1L ),
                        tuple( BlacklistedEntity.class, 1L ),
                        tuple( BlacklistedEntity.class, 2L ) );
    }
}