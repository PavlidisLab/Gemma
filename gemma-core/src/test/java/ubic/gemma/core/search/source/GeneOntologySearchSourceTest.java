package ubic.gemma.core.search.source;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.basecode.ontology.search.OntologySearchResult;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.util.test.BaseTest;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.gene.GeneSearchService;

import java.util.Collections;

import static org.mockito.Mockito.*;

@ContextConfiguration
public class GeneOntologySearchSourceTest extends BaseTest {

    @Configuration
    @TestComponent
    static class GOSSTCC {

        @Bean
        public GeneOntologySearchSource GeneOntologySearchSource() {
            return new GeneOntologySearchSource();
        }

        @Bean
        public GeneOntologyService geneOntologyService() {
            return mock();
        }

        @Bean
        public GeneSearchService geneSearchService() {
            return mock();
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService() {
            return mock();
        }

        @Bean
        public ArrayDesignService arrayDesignService() {
            return mock();
        }
    }

    @Autowired
    private GeneOntologySearchSource geneOntologySearchSource;

    @Autowired
    private GeneOntologyService geneOntologyService;

    @After
    public void resetMocks() {
        reset( geneOntologyService );
    }

    @Test
    public void test() throws SearchException, OntologySearchException {
        SearchSettings settings = SearchSettings.geneSearch( "GO:000001", null );
        geneOntologySearchSource.searchGene( settings );
        verify( geneOntologyService ).findTerm( "GO:000001", 2000 );
        verify( geneOntologyService ).getGenes( "GO:000001", null );
        verifyNoMoreInteractions( geneOntologyService );
    }

    @Test
    public void testWithUri() throws SearchException, OntologySearchException {
        when( geneOntologyService.getTerm( "http://purl.obolibrary.org/obo/GO:000001" ) )
                .thenReturn( mock() );
        SearchSettings settings = SearchSettings.geneSearch( "http://purl.obolibrary.org/obo/GO:000001", null );
        geneOntologySearchSource.searchGene( settings );
        verify( geneOntologyService ).findTerm( "http://purl.obolibrary.org/obo/GO:000001", 2000 );
        verify( geneOntologyService ).getGenes( eq( "http://purl.obolibrary.org/obo/GO:000001" ), isNull() );
        verifyNoMoreInteractions( geneOntologyService );
    }

    @Test
    public void testWithFreeTextTerms() throws SearchException, OntologySearchException {
        when( geneOntologyService.findTerm( any(), anyInt() ) )
                .thenReturn( Collections.singleton( new OntologySearchResult<>( mock(), 1.0 ) ) );
        when( geneOntologyService.findTerm( eq( "\"synaptic transmission\"" ), anyInt() ) )
                .thenReturn( Collections.emptyList() );
        SearchSettings settings = SearchSettings.geneSearch( "synaptic transmission", null );
        geneOntologySearchSource.searchGene( settings );
        verify( geneOntologyService ).findTerm( "\"synaptic transmission\"", 2000 );
        verify( geneOntologyService ).findTerm( "synaptic", 2000 );
        verify( geneOntologyService ).findTerm( "transmission", 2000 );
        verify( geneOntologyService, times( 2 ) ).getGenes( any( OntologyTerm.class ), isNull() );
        verifyNoMoreInteractions( geneOntologyService );
    }
}