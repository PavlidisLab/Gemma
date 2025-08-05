package ubic.gemma.persistence.hibernate;

import org.hibernate.Query;
import org.junit.Test;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@ContextConfiguration
public class HibernateUtilsTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class CC extends BaseDatabaseTestContextConfiguration {

    }

    @Test
    public void testIsStateless() {
        Query query = sessionFactory.getCurrentSession().createQuery( "select ee from ExpressionExperiment ee" );
        assertFalse( HibernateUtils.isStateless( query, sessionFactory ) );

        query = sessionFactory.getCurrentSession().createQuery( "select ee.id from ExpressionExperiment ee" );
        assertTrue( HibernateUtils.isStateless( query, sessionFactory ) );

        // mixing stateful and stateless
        query = sessionFactory.getCurrentSession().createQuery( "select ee, 1 from ExpressionExperiment ee" );
        assertFalse( HibernateUtils.isStateless( query, sessionFactory ) );

        query = sessionFactory.getCurrentSession().createQuery( "select c from Characteristic c" );
        assertTrue( HibernateUtils.isStateless( query, sessionFactory ) );

        // querying a collection of stateful entities
        query = sessionFactory.getCurrentSession().createQuery( "select ee.bioAssays from ExpressionExperiment ee" );
        assertFalse( HibernateUtils.isStateless( query, sessionFactory ) );

        // querying a collection of stateless entities
        query = sessionFactory.getCurrentSession().createQuery( "select ee.characteristics from ExpressionExperiment ee" );
        assertTrue( HibernateUtils.isStateless( query, sessionFactory ) );

        query = sessionFactory.getCurrentSession().createQuery( "select c, c, 2 from Characteristic c" );
        assertTrue( HibernateUtils.isStateless( query, sessionFactory ) );

        query = sessionFactory.getCurrentSession().createQuery( "select 1 from ExpressionExperiment ee" );
        assertTrue( HibernateUtils.isStateless( query, sessionFactory ) );

        query = sessionFactory.getCurrentSession().createQuery( "select scd.cellIds from SingleCellDimension scd" );
        assertTrue( HibernateUtils.isStateless( query, sessionFactory ) );

        query = sessionFactory.getCurrentSession().createQuery( "select scdv.dataIndices from SingleCellExpressionDataVector scdv" );
        assertTrue( HibernateUtils.isStateless( query, sessionFactory ) );
    }
}