package ubic.gemma.persistence.util;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import static ubic.gemma.persistence.util.AclCriteriaUtils.formAclRestrictionClause;

public class AclCriteriaUtilsTest extends BaseSpringContextTest {

    @Autowired
    private SessionFactory sessionFactory;

    @Test
    public void testAsAdmin() {
        super.runAsAdmin();
        sessionFactory.openSession().createCriteria( ExpressionExperiment.class )
                .add( formAclRestrictionClause( "id", ExpressionExperiment.class ) )
                .setMaxResults( 1 )
                .list();
    }

    @Test
    public void tesAsUser() {
        super.runAsUser( "bob", true );
        sessionFactory.openSession().createCriteria( ExpressionExperiment.class )
                .add( formAclRestrictionClause( "id", ExpressionExperiment.class ) )
                .setMaxResults( 1 )
                .list();
    }

    @Test
    public void test() {
        super.runAsAnonymous();
        sessionFactory.openSession().createCriteria( ExpressionExperiment.class )
                .add( formAclRestrictionClause( "id", ExpressionExperiment.class ) )
                .setMaxResults( 1 )
                .list();
    }
}