package ubic.gemma.core.search.source;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.model.OntologyTermSimple;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.common.search.Highlighter;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.util.TestComponent;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static ubic.gemma.core.search.source.OntologySearchSource.getLabelFromTermUri;

@ContextConfiguration
public class OntologySearchSourceTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class OntologySearchSourceImplTestContextConfiguration {

        @Bean
        public OntologySearchSource ontologySearchSource() {
            return new OntologySearchSource();
        }

        @Bean
        public OntologyService ontologyService() {
            return mock( OntologyService.class );
        }

        @Bean
        public CharacteristicService characteristicService() {
            return mock( CharacteristicService.class );
        }
    }

    @Autowired
    private SearchSource ontologySearchSource;


    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private CharacteristicService characteristicService;

    @After
    public void tearDown() {
        reset( ontologyService, characteristicService );
    }

    @Test
    public void test() throws SearchException, OntologySearchException {
        OntologyTerm term = new OntologyTermSimple( "http://purl.obolibrary.org/obo/CL_0000129", "microglial cell" );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        when( ontologyService.getResource( "http://purl.obolibrary.org/obo/CL_0000129" ) )
                .thenReturn( term );
        when( characteristicService.findExperimentsByUris( Collections.singleton( "http://purl.obolibrary.org/obo/CL_0000129" ), null, 5000, true, false ) )
                .thenReturn( Collections.singletonMap( ExpressionExperiment.class,
                        Collections.singletonMap( "http://purl.obolibrary.org/obo/CL_0000129", Collections.singleton( ee ) ) ) );
        Collection<SearchResult<ExpressionExperiment>> results = ontologySearchSource.searchExpressionExperiment( SearchSettings.expressionExperimentSearch( "http://purl.obolibrary.org/obo/CL_0000129" )
                .withHighlighter( new Highlighter() {
                    @Nullable
                    @Override
                    public String highlightTerm( String termUri, String termLabel, MessageSourceResolvable className ) {
                        return String.format( "[%s](%s)", termLabel, termUri );
                    }
                } ) );
        verify( ontologyService ).findIndividuals( "http://purl.obolibrary.org/obo/CL_0000129" );
        verify( ontologyService ).findTerms( "http://purl.obolibrary.org/obo/CL_0000129" );
        verify( characteristicService ).findExperimentsByUris( Collections.singleton( "http://purl.obolibrary.org/obo/CL_0000129" ), null, 5000, true, false );
        assertThat( results ).anySatisfy( result -> {
            assertThat( result )
                    .hasFieldOrPropertyWithValue( "resultType", ExpressionExperiment.class )
                    .hasFieldOrPropertyWithValue( "resultId", 1L );
            assertThat( result.getHighlights() )
                    .containsEntry( "term", "[microglial cell](http://purl.obolibrary.org/obo/CL_0000129)" );
        } );
    }

    @Test
    public void testWhenTermIsNotFoundGenerateLabelFromUri() throws SearchException, OntologySearchException {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        when( characteristicService.findExperimentsByUris( Collections.singleton( "http://purl.obolibrary.org/obo/CL_0000129" ), null, 5000, true, false ) )
                .thenReturn( Collections.singletonMap( ExpressionExperiment.class,
                        Collections.singletonMap( "http://purl.obolibrary.org/obo/CL_0000129", Collections.singleton( ee ) ) ) );
        Collection<SearchResult<ExpressionExperiment>> results = ontologySearchSource.searchExpressionExperiment( SearchSettings.expressionExperimentSearch( "http://purl.obolibrary.org/obo/CL_0000129" )
                .withHighlighter( new Highlighter() {
                    @Nullable
                    @Override
                    public String highlightTerm( String termUri, String termLabel, MessageSourceResolvable className ) {
                        return String.format( "[%s](%s)", termLabel, termUri );
                    }
                } ) );
        verify( ontologyService ).findIndividuals( "http://purl.obolibrary.org/obo/CL_0000129" );
        verify( ontologyService ).findTerms( "http://purl.obolibrary.org/obo/CL_0000129" );
        verify( characteristicService ).findBestByUri( "http://purl.obolibrary.org/obo/CL_0000129" );
        verify( characteristicService ).findExperimentsByUris( Collections.singleton( "http://purl.obolibrary.org/obo/CL_0000129" ), null, 5000, true, false );
        assertThat( results ).anySatisfy( result -> {
            assertThat( result )
                    .hasFieldOrPropertyWithValue( "resultType", ExpressionExperiment.class )
                    .hasFieldOrPropertyWithValue( "resultId", 1L );
            assertThat( result.getHighlights() )
                    .containsEntry( "term", "[CL:0000129](http://purl.obolibrary.org/obo/CL_0000129)" );
        } );
    }

    @Test
    public void testGetLabelFromTermUri() {
        assertEquals( "GO:0004016", getLabelFromTermUri( "http://purl.obolibrary.org/obo/GO_0004016" ) );
        assertEquals( "CHEBI:7466", getLabelFromTermUri( "http://purl.obolibrary.org/obo/chebi.owl#CHEBI_7466" ) );
        assertEquals( "BIRNLEX:15001", getLabelFromTermUri( "http://ontology.neuinfo.org/NIF/Function/NIF-Function.owl#birnlex_15001" ) );
        assertEquals( "GO:0004016", getLabelFromTermUri( "http://purl.obolibrary.org/obo//GO_0004016//" ) );
        assertEquals( "http://purl.obolibrary.org//", getLabelFromTermUri( "http://purl.obolibrary.org////" ) );
        assertEquals( "PAT:ID_20327", getLabelFromTermUri( "http://www.orphanet.org/rdfns#pat_id_20327" ) );
        assertEquals( "PAT:ID_20327", getLabelFromTermUri( "http://www.orphanet.org/rdfns#pat_id_20327" ) );
        assertEquals( "63857", getLabelFromTermUri( "http://purl.org/commons/record/ncbi_gene/63857" ) );
    }
}