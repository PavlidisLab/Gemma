package edu.columbia.gemma.interceptor;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.common.auditAndSecurity.AuditAction;
import edu.columbia.gemma.common.auditAndSecurity.AuditEvent;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrail;
import edu.columbia.gemma.common.auditAndSecurity.AuditTrailDao;
import edu.columbia.gemma.security.interceptor.PersistAclInterceptorBackend;

/**
 * Add to the audit trail on create, delete, or update of domain objects.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 * @spring.bean id="auditTrailInterceptor"
 * @spring.property name="auditTrailDao" ref="auditTrailDao"
 */
public class AuditTrailInterceptor implements MethodInterceptor {
    private static Log log = LogFactory.getLog( PersistAclInterceptorBackend.class.getName() );

    private AuditTrailDao auditTrailDao = null;

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

    /**
     * 'Around advice' method.
     * 
     * @param invocation
     * @return Object
     * @throws Throwable
     */
    public Object invoke( MethodInvocation invocation ) throws Throwable {

        Method method = invocation.getMethod();
        String mname = method.getName();

        log.debug( "before invocation.proceed(): method=[" + mname + "]" );

        Object retVal = invocation.proceed();

        log.debug( "after invocation.proceed(): retVal= " + retVal );

        Object[] arguments = invocation.getArguments();
        Object argument = arguments[0];

        Method mutator = argument.getClass().getMethod( "getAuditTrail" );

        assert mutator != null : mutator + " does not exist";

        AuditTrail auditTrail = ( AuditTrail ) mutator.invoke( argument, new Object[] {} );

        // Check if an auditTrail has been started for this object.
        if ( auditTrail == null ) {
            auditTrail = AuditTrail.Factory.newInstance();
            auditTrail.start();

        } else {

            // TODO get hook to the performer.
            Collection<AuditEvent> auditEvents = auditTrail.getEvents();
            AuditEvent auditEvent = AuditEvent.Factory.newInstance();
            auditEvent.setDate( new Date() );
            // auditEvent.setNote();
            // auditEvent.setPerformer();

            if ( mname.startsWith( "save" ) )
                auditEvent.setAction( AuditAction.CREATE );
            else if ( mname.toString().startsWith( "update" ) ) auditEvent.setAction( AuditAction.UPDATE );
            // else if ( mname.toString().startsWith( "remove" ) ) auditEvent.setAction( AuditAction.DELETE );

            auditEvents.add( auditEvent );

            auditTrail.setEvents( auditEvents );
        }

        Method accessor = null;
        Method[] methods = argument.getClass().getMethods();

        for ( int i = 0; i < methods.length; i++ ) {
            if ( methods[i].getName().equals( "setAuditTrail" ) ) {
                accessor = methods[i];
                break;
            }
        }

        assert accessor.getName().equals( "setAuditTrail" ) : argument.getClass() + "does not contain setAuditTrail";
        accessor.invoke( argument, new Object[] { auditTrail } );

        return retVal;
    }
}
