package edu.columbia.gemma.expression.experiment;

import java.util.Collection;

import org.hibernate.Session;

import edu.columbia.gemma.BaseDAOTestCase;
import edu.columbia.gemma.common.auditAndSecurity.Person;
import edu.columbia.gemma.common.auditAndSecurity.PersonDao;

/**
 * <hr>
 * <p>
 * Copyright (c) 2004, 2005 Columbia University
 * 
 * @author daq2101
 * @version $Id$
 */
public class ExpressionExperimentServiceImplTest extends BaseDAOTestCase {

    private ExpressionExperiment ee = null;
    private PersonDao pDao = null;
    private Person nobody = null;
    private Person admin = null;
    private ExpressionExperimentService svc = null;
    private long eeID;
    long nobodyID;
    long adminID;

    protected void setUp() throws Exception {

        super.setUp();

        // sessionFactory = (SessionFactory) ctx.getBean("sessionFactory");
        // s = sessionFactory.openSession();
        // TransactionSynchronizationManager.bindResource(sessionFactory, new SessionHolder(s));

        pDao = ( PersonDao ) ctx.getBean( "personDao" );
        nobody = ( Person ) pDao.load( new Long( 1 ) );
        admin = ( Person ) pDao.load( new Long( 2 ) );
        nobodyID = nobody.getId().longValue();
        adminID = admin.getId().longValue();
        svc = ( ExpressionExperimentService ) ctx.getBean( "expressionExperimentService" );
    }

    @SuppressWarnings("unchecked")
    public void testExpressionExperiment() {

        ee = ExpressionExperiment.Factory.newInstance();
        ee.setDescription( "From test" );
        ee.setName( "Test experiment" );
        ee.setOwner( nobody );

        ee.getInvestigators().add( admin );
        ee = svc.createExpressionExperiment( ee );

        eeID = ee.getId().longValue();

        Collection c = svc.findByInvestigator( adminID );
        assertTrue( c.size() > 0 );

        c = svc.getAllExpressionExperiments();
        assertTrue( c.size() > 0 );

        ee = svc.findById( eeID );
        assertTrue( ee != null && ee.getId().longValue() == eeID );
        c = ee.getInvestigators();
        // if( c != null)
        // System.out.println( c.size() );
        assertTrue( c != null && c.size() > 0 );
        ee.setName( "Test Experiment modified" );
        svc.saveExpressionExperiment( ee );
        svc.removeExpressionExperiment( ee );

        ee = svc.findById( eeID );
        assertNull( ee );

    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // SessionHolder holder = (SessionHolder) TransactionSynchronizationManager.getResource(sessionFactory);
        // s = holder.getSession();
        // s.flush();
        // TransactionSynchronizationManager.unbindResource(sessionFactory);
        // s.close();

    }
}