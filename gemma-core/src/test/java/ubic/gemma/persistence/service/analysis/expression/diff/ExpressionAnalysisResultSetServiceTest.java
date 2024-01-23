package ubic.gemma.persistence.service.analysis.expression.diff;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This implementation is using the Hibernate Criteria API, so we want to minimally test the logic of translating
 * {@link Filters} and {@link Sort} into proper {@link org.hibernate.Criteria} queries.
 * @author poirigui
 */
public class ExpressionAnalysisResultSetServiceTest extends BaseSpringContextTest {

    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;

    @Test
    public void testLoadValueObjects() {
        Filters filters = Filters.by( expressionAnalysisResultSetService.getFilter( "id", Filter.Operator.in, Collections.singletonList( "1123898912" ) ) );
        Sort sort = expressionAnalysisResultSetService.getSort( "id", Sort.Direction.DESC );
        List<DifferentialExpressionAnalysisResultSetValueObject> results = expressionAnalysisResultSetService.loadValueObjects( filters, sort );
        assertThat( results ).isEmpty();
    }

    @Test
    public void testLoadValueObjectsWithPagination() {
        Filters filters = Filters.by( expressionAnalysisResultSetService.getFilter( "id", Filter.Operator.in, Collections.singletonList( "1123898912" ) ) );
        Sort sort = expressionAnalysisResultSetService.getSort( "id", Sort.Direction.DESC );
        Slice<DifferentialExpressionAnalysisResultSetValueObject> results = expressionAnalysisResultSetService.loadValueObjects( filters, sort, 10, 20 );
        assertThat( results ).isEmpty();
    }

    @Test
    public void testFilterVosByNumberOfResults() {
        validateSizeProperty( "results.size", null, "results.size" );
    }

    @Test
    @Ignore("See https://github.com/PavlidisLab/Gemma/issues/518")
    public void testFilterVosByNumberOfCharacteristics() {
        validateSizeProperty( "analysis.experimentAnalyzed.characteristics.size", "e", "characteristics.size" );
    }

    /**
     * There's a few quirks for the Criteria API when it comes to treating size.
     */
    @Test
    public void testFilterBySize() {
        validateSizeProperty( "analysis.subsetFactorValue.characteristics.size", "sfv", "characteristics.size" );
        validateSizeProperty( "baselineGroup.characteristics.size", "b", "characteristics.size" );
    }

    private void validateSizeProperty( String property, @Nullable String expectedAlias, String expectedPropertyName ) {
        log.info( property );
        Filter f = expressionAnalysisResultSetService.getFilter( property, Filter.Operator.greaterOrEq, "2" );
        assertThat( f )
                .hasFieldOrPropertyWithValue( "objectAlias", expectedAlias )
                .hasFieldOrPropertyWithValue( "propertyName", expectedPropertyName );
        expressionAnalysisResultSetService.loadValueObjects( Filters.by( f ), null );
    }

    @Test
    public void testFilterableProperties() {
        assertThat( expressionAnalysisResultSetService.getFilterableProperties() )
                .contains( "analysis.id", "analysis.numberOfElementsAnalyzed" )
                .contains( "analysis.subsetFactorValue.characteristics.id" )
                .contains( "baselineGroup.characteristics.id" )
                .contains( "baselineGroup.measurement.type", "baselineGroup.measurement.kindCV", "baselineGroup.measurement.representation" )
                .doesNotContain( "analysis.name", "analysis.description" )
                .doesNotContain( "protocol.id", "protocol.name", "protocol.description" );
    }
}