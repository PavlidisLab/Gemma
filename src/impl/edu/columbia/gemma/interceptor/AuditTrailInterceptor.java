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
     * TODO finish implementation
     */
    public Object invoke( MethodInvocation invocation ) throws Throwable {

        Method m = invocation.getMethod();

        log.debug( "Before: method=[" + m + "]" );

        log.debug( "arguments " + invocation.getArguments() );

        Object retVal = invocation.proceed();

        log.debug( "After" );

        Collection<AuditTrail> col = auditTrailDao.findAllAuditTrails();

        for ( AuditTrail at : col ) {

        }

        AuditTrail auditTrail = AuditTrail.Factory.newInstance();

        AuditEvent auditEvent = AuditEvent.Factory.newInstance();

        auditEvent.setDate( new Date() );

        if ( m.toString().startsWith( "save" ) )
            auditEvent.setAction( AuditAction.CREATE );

        else if ( m.toString().startsWith( "update" ) )
            auditEvent.setAction( AuditAction.DELETE );

        else if ( m.toString().startsWith( "remove" ) ) auditEvent.setAction( AuditAction.DELETE );

        // Object[] methodArgs = invocation.getArguments();
        // Object obj = methodArgs[0];
        //
        // Method method = obj.getClass().getMethod( "setAuditTrail", new Class[] { auditTrail.getClass() } );

        return retVal;
    }

}
