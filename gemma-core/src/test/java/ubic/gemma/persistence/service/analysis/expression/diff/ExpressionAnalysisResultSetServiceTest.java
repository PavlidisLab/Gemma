package ubic.gemma.persistence.service.analysis.expression.diff;

import com.google.common.collect.Lists;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultSetValueObject;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

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
        Filters filters = Filters.by( expressionAnalysisResultSetService.getFilter( "id", Filter.Operator.in, Lists.newArrayList( "1123898912" ) ) );
        Sort sort = expressionAnalysisResultSetService.getSort( "id", Sort.Direction.DESC );
        List<DifferentialExpressionAnalysisResultSetValueObject> results = expressionAnalysisResultSetService.loadValueObjects( filters, sort );
        assertThat( results ).isEmpty();
    }

    @Test
    public void testLoadValueObjectsWithPagination() {
        Filters filters = Filters.by( expressionAnalysisResultSetService.getFilter( "id", Filter.Operator.in, Lists.newArrayList( "1123898912" ) ) );
        Sort sort = expressionAnalysisResultSetService.getSort( "id", Sort.Direction.DESC );
        Slice<DifferentialExpressionAnalysisResultSetValueObject> results = expressionAnalysisResultSetService.loadValueObjects( filters, sort, 10, 20 );
        assertThat( results ).isEmpty();
    }

    @Test
    public void testFilterVosByNumberOfResults() {
        expressionAnalysisResultSetService.loadValueObjects(
                Filters.by( null, "results.size", Integer.class, Filter.Operator.greaterOrEq, 2 ),
                null );
    }

    @Test
    @Ignore("See https://github.com/PavlidisLab/Gemma/issues/518")
    public void testFilterVosByNumberOfCharacteristics() {
        expressionAnalysisResultSetService.loadValueObjects(
                Filters.by( null, "analysis.experimentAnalyzed.characteristics.size", Integer.class, Filter.Operator.greaterOrEq, 2 ),
                null );
    }
}