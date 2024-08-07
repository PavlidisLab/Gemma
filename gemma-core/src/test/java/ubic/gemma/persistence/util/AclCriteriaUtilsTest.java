package ubic.gemma.persistence.util;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.core.util.test.RunAs;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import static ubic.gemma.persistence.util.AclCriteriaUtils.formAclRestrictionClause;

public class AclCriteriaUtilsTest extends BaseSpringContextTest {

    @Autowired
    private SessionFactory sessionFactory;

    private Session session;

    @Before
    public void setUp() {
        this.session = sessionFactory.openSession();
    }

    @After
    public void tearDown() {
        this.session.close();
    }

    @Test
    @RunAs(RunAs.Role.ADMIN)
    public void testAsAdmin() {
        session.createCriteria( ExpressionExperiment.class )
                .add( formAclRestrictionClause( "id", ExpressionExperiment.class ) )
                .setMaxResults( 1 )
                .list();
    }

    @Test
    public void tesAsUser() {
        super.runAsUser( "bob", true );
        session.createCriteria( ExpressionExperiment.class )
                .add( formAclRestrictionClause( "id", ExpressionExperiment.class ) )
                .setMaxResults( 1 )
                .list();
    }

    @Test
    @RunAs(RunAs.Role.ANONYMOUS)
    public void test() {
        session.createCriteria( ExpressionExperiment.class )
                .add( formAclRestrictionClause( "id", ExpressionExperiment.class ) )
                .setMaxResults( 1 )
                .list();
    }
}