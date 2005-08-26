package edu.columbia.gemma.common.auditAndSecurity;

import java.util.Collection;
import java.util.Date;

import org.hibernate.SessionFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.BaseDAOTestCase;

/**
 * This test is actually used to test sorting on collections. I am testing the Andromda AssociationEnd
 * andromda.hibernate.orderByColumns tagged value. This value is part of the sterotype EntityAssociation.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class AuditTrailDaoTest extends BaseDAOTestCase {
    protected static final Log log = LogFactory.getLog( AuditTrailDaoTest.class );

    SessionFactory sf;
    AuditTrailDao auditTrailDao;

    AuditTrail auditTrail;
    AuditEvent auditEvent0;
    AuditEvent auditEvent1;
    AuditEvent auditEvent2;
    AuditEvent auditEvent3;
    AuditEvent auditEvent4;

    /**
     * @exception Exception
     */
    protected void setUp() throws Exception {

        super.setUp();

        sf = ( SessionFactory ) ctx.getBean( "sessionFactory" );
        setAuditTrailDao( ( AuditTrailDao ) ctx.getBean( "auditTrailDao" ) );

        auditTrail = AuditTrail.Factory.newInstance();

        auditEvent0 = AuditEvent.Factory.newInstance();
        auditEvent0.setDate( new Date() );
        auditEvent0.setNote( "ccccc" );
        auditEvent0.setAction( AuditAction.CREATE );

        auditEvent1 = AuditEvent.Factory.newInstance();
        auditEvent1.setDate( new Date() );
        auditEvent1.setNote( "ddddd" );
        auditEvent1.setAction( AuditAction.CREATE );

        auditEvent2 = AuditEvent.Factory.newInstance();
        auditEvent2.setDate( new Date() );
        auditEvent2.setNote( "aaaaa" );
        auditEvent2.setAction( AuditAction.CREATE );

        auditEvent3 = AuditEvent.Factory.newInstance();
        auditEvent3.setDate( new Date() );
        auditEvent3.setNote( "bbbbb" );
        auditEvent3.setAction( AuditAction.CREATE );

        auditTrail.addEvent( auditEvent0 );
        auditTrail.addEvent( auditEvent1 );
        auditTrail.addEvent( auditEvent2 );
        auditTrail.addEvent( auditEvent3 );

    }

    /**
     * @exception Exception
     */
    protected void tearDown() throws Exception {
        // getAuditTrailDao().remove( auditTrail );
        auditTrailDao = null;
    }

    public void testCreate() {

        log.info( "Creating audit trail" );

        getAuditTrailDao().create( auditTrail );
    }

    /**
     * this method retrieves all
     */
    public void testfindAllAuditTrails() {
        log.info( "Retreiving all audit events" );

        Collection<AuditTrail> trails = getAuditTrailDao().findAll();
        for ( AuditTrail at : trails ) {
            log.info( at );
            Collection<AuditEvent> events = at.getEvents();
            for ( AuditEvent ae : events ) {
                log.info( ae.getNote() );
            }

        }
    }

    /**
     * @return Returns the auditTrailDao.
     */
    public AuditTrailDao getAuditTrailDao() {
        return auditTrailDao;
    }

    /**
     * @param auditTrailDao The auditTrailDao to set.
     */
    public void setAuditTrailDao( AuditTrailDao auditTrailDao ) {
        this.auditTrailDao = auditTrailDao;
    }
}
