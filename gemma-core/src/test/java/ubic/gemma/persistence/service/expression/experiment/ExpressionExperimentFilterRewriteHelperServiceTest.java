package ubic.gemma.persistence.service.expression.experiment;

import org.assertj.core.util.Sets;
import org.junit.Before;
import org.junit.Test;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.simple.OntologyTermSimple;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Subquery;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ExpressionExperimentFilterRewriteHelperServiceTest {

    private ExpressionExperimentFilterRewriteHelperService filterInferenceService;
    private OntologyService ontologyService;
    private OntologyTerm term, b, c;

    @Before
    public void setUp() throws TimeoutException {
        ontologyService = mock();
        filterInferenceService = new ExpressionExperimentFilterRewriteHelperService( ontologyService );
        term = new OntologyTermSimple( "http://example.com", null );
        b = new OntologyTermSimple( "http://example.com/a", null );
        c = new OntologyTermSimple( "http://example.com/b", null );
        when( ontologyService.getTerms( eq( Collections.singleton( "http://example.com" ) ), anyLong(), any() ) ).thenReturn( Collections.singleton( term ) );
        when( ontologyService.getChildren( eq( Collections.singleton( term ) ), eq( false ), eq( true ), anyLong(), any() ) )
                .thenReturn( Sets.set( b, c ) );
    }

    @Test
    public void test() throws TimeoutException {
        Filters filters = Filters.by( "ac", "valueUri", String.class, Filter.Operator.eq, "http://example.com", "allCharacteristics.valueUri" );
        Filters inferredFilters = filterInferenceService.getFiltersWithInferredAnnotations( filters, "ee", null, null, 30, TimeUnit.SECONDS );
        assertThat( inferredFilters.toOriginalString() ).isEqualTo( "any(allCharacteristics.valueUri in (http://example.com, http://example.com/a, http://example.com/b))" );
        verify( ontologyService ).getTerms( eq( Collections.singleton( "http://example.com" ) ), anyLong(), any() );
        verify( ontologyService ).getChildren( eq( Collections.singleton( term ) ), eq( false ), eq( true ), anyLong(), any() );
    }

    @Test
    public void testIn() throws TimeoutException {
        Filters filters = Filters.by( "ac", "valueUri", String.class, Filter.Operator.in, Collections.singleton( "http://example.com" ), "allCharacteristics.valueUri" );
        Filters inferredFilters = filterInferenceService.getFiltersWithInferredAnnotations( filters, "ee", null, null, 30, TimeUnit.SECONDS );
        assertThat( inferredFilters.toOriginalString() ).isEqualTo( "any(allCharacteristics.valueUri in (http://example.com, http://example.com/a, http://example.com/b))" );
        verify( ontologyService ).getTerms( eq( Collections.singleton( "http://example.com" ) ), anyLong(), any() );
        verify( ontologyService ).getChildren( eq( Collections.singleton( term ) ), eq( false ), eq( true ), anyLong(), any() );
    }

    @Test
    public void testNotEq() throws TimeoutException {
        Filters filters = Filters.by( "ac", "valueUri", String.class, Filter.Operator.notEq, "http://example.com", "allCharacteristics.valueUri" );
        Filters inferredFilters = filterInferenceService.getFiltersWithInferredAnnotations( filters, "ee", null, null, 30, TimeUnit.SECONDS );
        assertThat( inferredFilters.toOriginalString() ).isEqualTo( "none(allCharacteristics.valueUri in (http://example.com, http://example.com/a, http://example.com/b))" );
        verify( ontologyService ).getTerms( eq( Collections.singleton( "http://example.com" ) ), anyLong(), any() );
        verify( ontologyService ).getChildren( eq( Collections.singleton( term ) ), eq( false ), eq( true ), anyLong(), any() );
    }

    @Test
    public void testNotIn() throws TimeoutException {
        Filters filters = Filters.by( "ac", "valueUri", String.class, Filter.Operator.notIn, Collections.singleton( "http://example.com" ), "allCharacteristics.valueUri" );
        Filters inferredFilters = filterInferenceService.getFiltersWithInferredAnnotations( filters, "ee", null, null, 30, TimeUnit.SECONDS );
        assertThat( inferredFilters.toOriginalString() ).isEqualTo( "none(allCharacteristics.valueUri in (http://example.com, http://example.com/a, http://example.com/b))" );
        verify( ontologyService ).getTerms( eq( Collections.singleton( "http://example.com" ) ), anyLong(), any() );
        verify( ontologyService ).getChildren( eq( Collections.singleton( term ) ), eq( false ), eq( true ), anyLong(), any() );
    }

    @Test
    public void testInSubquery() throws TimeoutException {
        List<Subquery.Alias> aliases = Arrays.asList( new Subquery.Alias( null, "allCharacteristics", "ac" ) );
        Filters filters = Filters.by( "ee", "id", String.class, Filter.Operator.inSubquery, new Subquery( "ExpressionExperiment", "id", aliases, Filter.by( "ac", "valueUri", String.class, Filter.Operator.eq, "http://example.com", "allCharacteristics.valueUri" ) ), "allCharacteristics.valueUri" );
        Filters inferredFilters = filterInferenceService.getFiltersWithInferredAnnotations( filters, "ee", null, null, 30, TimeUnit.SECONDS );
        verify( ontologyService ).getTerms( eq( Collections.singleton( "http://example.com" ) ), anyLong(), any() );
        verify( ontologyService ).getChildren( eq( Collections.singleton( term ) ), eq( false ), eq( true ), anyLong(), any() );
        assertThat( inferredFilters.toOriginalString() ).isEqualTo( "any(allCharacteristics.valueUri in (http://example.com, http://example.com/a, http://example.com/b))" );
    }

    @Test
    public void testNotInSubquery() throws TimeoutException {
        List<Subquery.Alias> aliases = Arrays.asList( new Subquery.Alias( null, "allCharacteristics", "ac" ) );
        Filters filters = Filters.by( "ee", "id", String.class, Filter.Operator.notInSubquery, new Subquery( "ExpressionExperiment", "id", aliases, Filter.by( "ac", "valueUri", String.class, Filter.Operator.eq, "http://example.com", "allCharacteristics.valueUri" ) ), "allCharacteristics.valueUri" );
        Filters inferredFilters = filterInferenceService.getFiltersWithInferredAnnotations( filters, "ee", null, null, 30, TimeUnit.SECONDS );
        verify( ontologyService ).getTerms( eq( Collections.singleton( "http://example.com" ) ), anyLong(), any() );
        verify( ontologyService ).getChildren( eq( Collections.singleton( term ) ), eq( false ), eq( true ), anyLong(), any() );
        assertThat( inferredFilters.toOriginalString() ).isEqualTo( "none(allCharacteristics.valueUri in (http://example.com, http://example.com/a, http://example.com/b))" );
    }
}