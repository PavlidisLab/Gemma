package ubic.gemma.persistence.service.analysis.expression.diff;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetValueObject;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;
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
    public void testLoadValueObjectsPreFilter() {
        Filters filters = Filters.singleFilter( expressionAnalysisResultSetService.getObjectFilter( "id", ObjectFilter.Operator.in, Lists.newArrayList( "1123898912" ) ) );
        Sort sort = expressionAnalysisResultSetService.getSort( "id", Sort.Direction.DESC );
        List<ExpressionAnalysisResultSetValueObject> results = expressionAnalysisResultSetService.loadValueObjectsPreFilter( filters, sort );
        assertThat( results ).isEmpty();
    }

    public void testLoadValueObjectsPreFilterWithPagination() {
        Filters filters = Filters.singleFilter( expressionAnalysisResultSetService.getObjectFilter( "id", ObjectFilter.Operator.in, Lists.newArrayList( "1123898912" ) ) );
        Sort sort = expressionAnalysisResultSetService.getSort( "id", Sort.Direction.DESC );
        Slice<ExpressionAnalysisResultSetValueObject> results = expressionAnalysisResultSetService.loadValueObjectsPreFilter( filters, sort, 10, 20 );
        assertThat( results ).isEmpty();
    }
}