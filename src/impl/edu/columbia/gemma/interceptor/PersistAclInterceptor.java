package edu.columbia.gemma.interceptor;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * This is the interceptor to call to persist acl information when an object is created.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class PersistAclInterceptor implements MethodInterceptor {
    private static Log log = LogFactory.getLog( PersistAclInterceptor.class.getName() );

    public Object invoke( MethodInvocation invocation ) throws Throwable {
        log.info( "Before: invocation=[" + invocation + "]" );

        log.info( invocation.getMethod());
        //TODO get the argument of the method, and persist this:id to acl object identity
        

        invocation.proceed();
        return null;
    }

}
