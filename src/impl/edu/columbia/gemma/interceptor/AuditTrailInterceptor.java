/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
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
    public static final String saveMethodPrefix = "save";
    public static final String updateMethodPrefix = "update";

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
     * 'Around advice' method. Inspects the service method, then either creates a new AuditTrail or adds an AuditEvent
     * to an existing AuditTrail for the target object.
     * 
     * @param invocation
     * @return Object
     * @throws Throwable
     */
    @SuppressWarnings("unchecked")
    public Object invoke( MethodInvocation invocation ) throws Throwable {

        Method method = invocation.getMethod();
        String mname = method.getName();

        Object retVal = null;

        retVal = invocation.proceed();

        if ( mname.startsWith( saveMethodPrefix ) || mname.startsWith( updateMethodPrefix ) ) {
            log.debug( "before invocation.proceed(): method=[" + mname + "]" );

            log.debug( "after invocation.proceed(): retVal= " + retVal );

            Object[] arguments = invocation.getArguments();
            Object argument = arguments[0];// domain object

            Method mutator = argument.getClass().getMethod( "getAuditTrail" );

            assert mutator != null : mutator + " does not exist";

            AuditTrail auditTrail = ( AuditTrail ) mutator.invoke( argument, new Object[] {} );

            // Check if an auditTrail has been started for this object.
            if ( auditTrail == null ) {
                auditTrail = AuditTrail.Factory.newInstance();
                auditTrail.start();

            } else {

                // TODO get hook to the performer. I need to find a way to get the note from the user.
                Collection<AuditEvent> auditEvents = auditTrail.getEvents();
                AuditEvent auditEvent = AuditEvent.Factory.newInstance();
                auditEvent.setDate( new Date() );
                // auditEvent.setNote();
                // auditEvent.setPerformer();

                if ( mname.startsWith( saveMethodPrefix ) )
                    auditEvent.setAction( AuditAction.CREATE );
                else if ( mname.toString().startsWith( updateMethodPrefix ) )
                    auditEvent.setAction( AuditAction.UPDATE );

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

            assert accessor.getName().equals( "setAuditTrail" ) : argument.getClass()
                    + "does not contain setAuditTrail";
            accessor.invoke( argument, new Object[] { auditTrail } );

        }
        return retVal;
    }
}
