package ubic.gemma.persistence.util;

import org.junit.Test;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import static ubic.gemma.persistence.util.AclCriteriaUtils.formAclRestrictionClause;

public class AclCriteriaUtilsTest extends BaseSpringContextTest {

    @Test
    public void testAsAdmin() {
        super.runAsAdmin();
        formAclRestrictionClause( "ee", ExpressionExperiment.class );
    }

    @Test
    public void test() {
        super.runAsAnonymous();
        formAclRestrictionClause( "ee", ExpressionExperiment.class );
    }
}