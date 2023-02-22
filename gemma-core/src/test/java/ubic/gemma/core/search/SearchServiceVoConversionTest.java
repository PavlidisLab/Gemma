package ubic.gemma.core.search;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.DatabaseBackedGeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.TestComponent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ContextConfiguration
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
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private GeneSetService geneSetService;

    /* fixtures */
    private ArrayDesign ad;
    private ExpressionExperiment ee;
    private GeneSet gs;
    private CharacteristicValueObject phenotypeAssociation;

    @Before
    public void setUp() {
        ad = new ArrayDesign();
        ad.setId( 11L );
        ad.setPrimaryTaxon( Taxon.Factory.newInstance( "Homo sapiens", "Human", 9606, false ) );
        ee = new ExpressionExperiment();
        ee.setId( 12L );
        gs = new GeneSet();
        gs.setId( 13L );
        phenotypeAssociation = new CharacteristicValueObject( 14L );
        when( arrayDesignService.loadValueObject( any( ArrayDesign.class ) ) ).thenAnswer( a -> new ArrayDesignValueObject( a.getArgument( 0, ArrayDesign.class ) ) );
        when( expressionExperimentService.loadValueObject( any( ExpressionExperiment.class ) ) ).thenAnswer( a -> new ExpressionExperimentValueObject( a.getArgument( 0, ExpressionExperiment.class ) ) );
        when( geneSetService.loadValueObject( any( GeneSet.class ) ) ).thenAnswer( a -> {
            GeneSet geneSet = a.getArgument( 0, GeneSet.class );
            return new DatabaseBackedGeneSetValueObject( geneSet );
        } );
    }


    @After
    public void tearDown() {
        reset( arrayDesignService, expressionExperimentService, geneSetService );
    }

    @Test
    public void testConvertArrayDesign() {
        searchService.loadValueObject( SearchResult.from( ArrayDesign.class, ad, 1.0, "test object" ) );
        verify( arrayDesignService ).loadValueObject( ad );
    }

    @Test
    public void testConvertExpressionExperiment() {
        searchService.loadValueObject( SearchResult.from( ExpressionExperiment.class, ee, 1.0, "test object" ) );
        verify( expressionExperimentService ).loadValueObject( ee );
    }

    @Test
    public void testConvertPhenotypeAssociation() {
        // this is a complicated one because
        assertThat( searchService.loadValueObject( SearchResult.from( PhenotypeAssociation.class, phenotypeAssociation, 1.0, "test object" ) ) )
                .extracting( "resultObject" )
                .isSameAs( phenotypeAssociation );
    }

    @Test
    public void testConvertGeneSet() {
        // this is another complicated one because GeneSetService does not implement BaseVoEnabledService
        searchService.loadValueObject( SearchResult.from( GeneSet.class, gs, 1.0, "test object" ) );
        verify( geneSetService ).loadValueObject( gs );
    }

    @Test(expected = IllegalArgumentException.class)
    public void testUnsupportedResultTypeRaisesIllegalArgumentException() {
        ContrastResult cr = new ContrastResult();
        cr.setId( 1L );
        searchService.loadValueObject( SearchResult.from( ContrastResult.class, cr, 1.0, "test object" ) );
    }

}